package com.example.networkmonitor.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkmonitor.data.AlertEntity
import com.example.networkmonitor.data.AppDatabase
import com.example.networkmonitor.data.AppSummary
import com.example.networkmonitor.data.ConnectionEntity
import com.example.networkmonitor.data.DomainSummary
import com.example.networkmonitor.data.NetworkRepository
import com.example.networkmonitor.util.ExportManager
import com.example.networkmonitor.vpn.NetworkMonitorVpnService
import com.example.networkmonitor.vpn.ThreatUpdater
import com.example.networkmonitor.data.UserRuleEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db.connectionDao(), db.alertDao(), db.threatDao(), db.userRuleDao())
    private val threatUpdater = ThreatUpdater(application, repository)

    // Real-time connection feed (most recent first)
    val connections: StateFlow<List<ConnectionEntity>> = repository
        .getAllConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Per-app summaries for the app list screen
    val appSummaries: StateFlow<List<AppSummary>> = repository
        .getAppSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Alert feed
    val alerts: StateFlow<List<AlertEntity>> = repository
        .getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread alert count for badge
    val unreadAlertCount: StateFlow<Int> = repository
        .getUnreadAlertCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // VPN running status (derived from the static flag on the service)
    val isVpnRunning: StateFlow<Boolean> = flow {
        while (true) {
            emit(NetworkMonitorVpnService.isRunning)
            kotlinx.coroutines.delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkMonitorVpnService.isRunning)

    // Search/filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredConnections: StateFlow<List<ConnectionEntity>> = connections
        .combine(searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.domain.contains(query, ignoreCase = true) ||
                        it.ip.contains(query, ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun getDomainsForApp(packageName: String): Flow<List<DomainSummary>> =
        repository.getDomainsForApp(packageName)

    fun markAlertRead(id: Long) = viewModelScope.launch {
        repository.markAlertRead(id)
    }

    fun markAllAlertsRead() = viewModelScope.launch {
        repository.markAllAlertsRead()
    }

    fun startVpn(): Intent? {
        return VpnService.prepare(getApplication())
    }

    fun stopVpn() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, NetworkMonitorVpnService::class.java).apply {
            action = NetworkMonitorVpnService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    fun launchVpnService() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, NetworkMonitorVpnService::class.java).apply {
            action = NetworkMonitorVpnService.ACTION_START
        }
        ctx.startForegroundService(intent)
    }

    fun exportJson(uri: Uri) = viewModelScope.launch {
        ExportManager.exportJson(getApplication(), connections.value, uri)
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        ExportManager.exportCsv(getApplication(), connections.value, uri)
    }

    // User actions
    fun blockTarget(target: String, isDomain: Boolean) = viewModelScope.launch {
        repository.insertRule(UserRuleEntity(target = target, isDomain = isDomain, action = "BLOCK"))
        markAllAlertsRead() // Mark read after taking action
    }

    fun ignoreTarget(target: String, isDomain: Boolean) = viewModelScope.launch {
        repository.insertRule(UserRuleEntity(target = target, isDomain = isDomain, action = "IGNORE"))
        markAllAlertsRead()
    }
}
