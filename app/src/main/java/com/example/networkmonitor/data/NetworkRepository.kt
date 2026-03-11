package com.example.networkmonitor.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NetworkRepository(
    private val connectionDao: ConnectionDao,
    private val alertDao: AlertDao,
    private val threatDao: ThreatDao,
    private val userRuleDao: UserRuleDao
) {
    // Connection queries
    fun getAllConnections(): Flow<List<ConnectionEntity>> = connectionDao.getAllConnections()

    fun getConnectionsForApp(packageName: String): Flow<List<ConnectionEntity>> =
        connectionDao.getConnectionsForApp(packageName)

    fun getAppSummaries(): Flow<List<AppSummary>> = connectionDao.getAppSummaries()

    fun getDomainsForApp(packageName: String): Flow<List<DomainSummary>> =
        connectionDao.getDomainsForApp(packageName)

    fun getConnectionsSince(since: Long): Flow<List<ConnectionEntity>> =
        connectionDao.getConnectionsSince(since)

    suspend fun insertConnection(connection: ConnectionEntity): Long =
        connectionDao.insertConnection(connection)

    suspend fun pruneOldConnections(olderThanMs: Long) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        connectionDao.deleteConnectionsBefore(cutoff)
    }

    // Alert queries
    fun getAllAlerts(): Flow<List<AlertEntity>> = alertDao.getAllAlerts()

    fun getUnreadAlertCount(): Flow<Int> = alertDao.getUnreadCount()

    suspend fun insertAlert(alert: AlertEntity): Long = alertDao.insertAlert(alert)

    suspend fun markAlertRead(id: Long) = alertDao.markAsRead(id)

    suspend fun markAllAlertsRead() = alertDao.markAllAsRead()

    suspend fun hasRecentAlert(domain: String, windowMs: Long = 60_000): Boolean {
        val since = System.currentTimeMillis() - windowMs
        return alertDao.countRecentAlertsForDomain(domain, since) > 0
    }

    // Threat queries
    suspend fun checkThreat(indicator: String): ThreatEntity? = threatDao.findThreat(indicator)
    fun getThreatCountFlow(): Flow<Int> = threatDao.getThreatCountFlow()
    suspend fun getThreatCount(): Int = threatDao.getThreatCount()
    suspend fun insertThreats(threats: List<ThreatEntity>) = threatDao.insertThreats(threats)
    suspend fun clearThreats() = threatDao.clearThreats()

    // User Rules
    fun getAllRules() = userRuleDao.getAllRules()
    suspend fun getAllRulesSync() = userRuleDao.getAllRulesSync()
    suspend fun insertRule(rule: UserRuleEntity) = userRuleDao.insertRule(rule)
    suspend fun deleteRule(target: String, isDomain: Boolean) = userRuleDao.deleteRule(target, isDomain)
}
