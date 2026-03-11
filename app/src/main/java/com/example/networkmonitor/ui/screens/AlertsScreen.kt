package com.example.networkmonitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networkmonitor.data.AlertEntity
import com.example.networkmonitor.ui.theme.*
import com.example.networkmonitor.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    val unread by viewModel.unreadAlertCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = WarnAmber, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Security Alerts", fontWeight = FontWeight.Bold, color = OnSurfaceText)
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("$unread") }
                    }
                }
            },
            actions = {
                if (alerts.isNotEmpty()) {
                    TextButton(onClick = { viewModel.markAllAlertsRead() }) {
                        Text("Mark all read", color = CyberGreen, fontSize = 12.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
        )

        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No security alerts", color = SubtleText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("All monitored traffic looks clean", color = SubtleText.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(
                        alert = alert,
                        onRead = { viewModel.markAlertRead(alert.id) },
                        onBlock = { viewModel.blockTarget(alert.domain, true) },
                        onIgnore = { viewModel.ignoreTarget(alert.domain, true) }
                    )
                    Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun AlertRow(
    alert: AlertEntity,
    onRead: () -> Unit,
    onBlock: () -> Unit,
    onIgnore: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val riskColor = when (alert.riskLevel) {
        4 -> DangerRed     // CRITICAL
        3 -> DangerRed     // HIGH
        2 -> WarnAmber     // MEDIUM
        else -> WarnAmber
    }
    val riskLabel = when (alert.riskLevel) {
        4 -> "🛑 Critical"
        3 -> "⛔ High Risk"
        2 -> "⚠ Medium Risk"
        else -> "⚠ Suspicious"
    }
    
    val timeLabel = formatRelativeTime(alert.timestamp)

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Threat Details", color = OnSurfaceText) },
            text = {
                Column {
                    Text("Domain: ${alert.domain}", color = OnSurfaceText)
                    Text("IP: ${alert.ip}", color = SubtleText)
                    Text("Targeted App: ${alert.appName}", color = SubtleText)
                    Spacer(Modifier.height(8.dp))
                    Text("Source: ${alert.riskSource}", color = riskColor)
                    Text("Explanation: ${alert.description}", color = OnSurfaceText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) { Text("Close", color = CyberGreen) }
            },
            containerColor = SurfaceCard
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!alert.isRead) onRead()
                showDetails = true
            },
        color = if (alert.isRead) SurfaceDark else SurfaceCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Risk icon
                Surface(
                    color = riskColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (alert.riskLevel >= 3) Icons.Default.GppBad else Icons.Default.GppMaybe,
                            null,
                            tint = riskColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = riskColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                riskLabel,
                                color = riskColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(timeLabel, color = SubtleText, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        alert.domain,
                        color = OnSurfaceText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${alert.appName} → ${alert.ip}",
                        color = SubtleText,
                        fontSize = 12.sp
                    )
                    if (alert.description.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            alert.description,
                            color = SubtleText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onIgnore) {
                    Text("Ignore", color = SubtleText)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.2f), contentColor = DangerRed)
                ) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Block Connection")
                }
            }
        }
    }
}
