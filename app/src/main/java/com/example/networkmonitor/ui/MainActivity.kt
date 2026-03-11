package com.example.networkmonitor.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.networkmonitor.ui.screens.AlertsScreen
import com.example.networkmonitor.ui.screens.AppDetailScreen
import com.example.networkmonitor.ui.screens.HistoryScreen
import com.example.networkmonitor.ui.screens.MonitorScreen
import com.example.networkmonitor.ui.theme.NetworkMonitorTheme
import com.example.networkmonitor.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkMonitorTheme {
                NetworkMonitorApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMonitorApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val unreadCount by viewModel.unreadAlertCount.collectAsState()
    val showUpdate by viewModel.showUpdatePrompt.collectAsState()

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.launchVpnService()
        }
    }

    // Export launchers
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJson(it) } }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(it) } }

    Scaffold(
        bottomBar = {
            AppBottomNav(navController = navController, unreadCount = unreadCount)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Monitor.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavRoute.Monitor.route) {
                MonitorScreen(
                    viewModel = viewModel,
                    onAppClick = { packageName ->
                        navController.navigate("${NavRoute.AppDetail.route}/$packageName")
                    },
                    onStartVpn = {
                        val prepareIntent = viewModel.startVpn()
                        if (prepareIntent != null) {
                            vpnPermissionLauncher.launch(prepareIntent)
                        } else {
                            viewModel.launchVpnService()
                        }
                    },
                    onStopVpn = { viewModel.stopVpn() },
                    onExportJson = { jsonExportLauncher.launch("network_log_${System.currentTimeMillis()}.json") },
                    onExportCsv = { csvExportLauncher.launch("network_log_${System.currentTimeMillis()}.csv") }
                )
            }
            composable("${NavRoute.AppDetail.route}/{packageName}") { backStack ->
                val pkg = backStack.arguments?.getString("packageName") ?: return@composable
                AppDetailScreen(viewModel = viewModel, packageName = pkg, onBack = { navController.popBackStack() })
            }
            composable(NavRoute.Alerts.route) {
                AlertsScreen(viewModel = viewModel)
            }
            composable(NavRoute.History.route) {
                HistoryScreen(viewModel = viewModel)
            }
        }

        if (showUpdate) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdatePrompt() },
                title = { Text("Security Database Update Available") },
                text = { Text("New threat intelligence data is available. Updating improves malicious site detection.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.performThreatUpdate() }) {
                        Text("Update Now", color = CyberGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                        Text("Remind Me Later", color = SubtleText)
                    }
                },
                containerColor = SurfaceCard,
                titleContentColor = OnSurfaceText,
                textContentColor = OnSurfaceText
            )
        }
    }
}

sealed class NavRoute(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Monitor : NavRoute("monitor", "Monitor", androidx.compose.material.icons.Icons.Default.Wifi)
    object AppDetail : NavRoute("app_detail", "Detail", androidx.compose.material.icons.Icons.Default.Apps)
    object Alerts : NavRoute("alerts", "Alerts", androidx.compose.material.icons.Icons.Default.Warning)
    object History : NavRoute("history", "History", androidx.compose.material.icons.Icons.Default.History)
}

@Composable
fun AppBottomNav(navController: androidx.navigation.NavHostController, unreadCount: Int) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(NavRoute.Monitor, NavRoute.Alerts, NavRoute.History)

    NavigationBar(containerColor = com.example.networkmonitor.ui.theme.SurfaceCard) {
        topLevelRoutes.forEach { route ->
            NavigationBarItem(
                selected = currentRoute == route.route,
                onClick = {
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (route == NavRoute.Alerts && unreadCount > 0) {
                        BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                            Icon(route.icon, contentDescription = route.label)
                        }
                    } else {
                        Icon(route.icon, contentDescription = route.label)
                    }
                },
                label = { Text(route.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.example.networkmonitor.ui.theme.CyberGreen,
                    selectedTextColor = com.example.networkmonitor.ui.theme.CyberGreen,
                    indicatorColor = com.example.networkmonitor.ui.theme.SurfaceElevated
                )
            )
        }
    }
}
