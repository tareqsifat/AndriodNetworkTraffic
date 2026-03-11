package com.example.networkmonitor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networkmonitor.data.ConnectionEntity
import com.example.networkmonitor.ui.theme.*
import com.example.networkmonitor.util.ExportManager
import com.example.networkmonitor.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MainViewModel,
    onAppClick: (String) -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit
) {
    val connections by viewModel.filteredConnections.collectAsState()
    val isRunning by viewModel.isVpnRunning.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showExportMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D2013), SurfaceDark)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Network Monitor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = OnSurfaceText
                            )
                            Text(
                                if (isRunning) "● Monitoring active" else "○ Monitoring off",
                                fontSize = 12.sp,
                                color = if (isRunning) CyberGreen else SubtleText
                            )
                        }
                    }

                    Row {
                        // Export menu
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.FileDownload, "Export", tint = SubtleText)
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                                modifier = Modifier.background(SurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export JSON", color = OnSurfaceText) },
                                    leadingIcon = { Icon(Icons.Default.Code, null, tint = CyberGreen) },
                                    onClick = { showExportMenu = false; onExportJson() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export CSV", color = OnSurfaceText) },
                                    leadingIcon = { Icon(Icons.Default.TableChart, null, tint = CyberGreen) },
                                    onClick = { showExportMenu = false; onExportCsv() }
                                )
                            }
                        }

                        // VPN toggle
                        FilledTonalButton(
                            onClick = { if (isRunning) onStopVpn() else onStartVpn() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isRunning) Color(0xFF3D0008) else Color(0xFF003314),
                                contentColor = if (isRunning) DangerRed else CyberGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRunning) "Stop" else "Start", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search apps, domains, IPs…", color = SubtleText, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = SubtleText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null, tint = SubtleText)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = OnSurfaceText,
                        unfocusedTextColor = OnSurfaceText,
                        cursorColor = CyberGreen
                    ),
                    singleLine = true
                )
            }
        }

        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StatChip("Total", connections.size.toString(), CyberGreen)
            StatChip("Suspicious", connections.count { it.riskLevel == 1 }.toString(), WarnAmber)
            StatChip("Malicious", connections.count { it.riskLevel == 2 }.toString(), DangerRed)
        }

        Divider(color = BorderColor, thickness = 1.dp)

        // Connection list
        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WifiOff,
                        null,
                        tint = SubtleText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isRunning) "No connections captured yet" else "Start monitoring to capture traffic",
                        color = SubtleText,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(connections, key = { it.id }) { conn ->
                    ConnectionRow(conn) { onAppClick(conn.packageName) }
                    Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: ConnectionEntity, onClick: () -> Unit) {
    val riskColor = when (conn.riskLevel) {
        2 -> DangerRed
        1 -> WarnAmber
        else -> SafeGreen
    }
    val riskLabel = ExportManager.riskLevelLabel(conn.riskLevel)
    val timeLabel = remember(conn.timestamp) {
        val diff = System.currentTimeMillis() - conn.timestamp
        when {
            diff < 60_000 -> "${diff / 1000}s ago"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conn.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Risk dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(riskColor)
        )
        Spacer(Modifier.width(12.dp))

        // App initial avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                conn.appName.firstOrNull()?.uppercase() ?: "?",
                color = CyberGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))

        // Main content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    conn.appName,
                    color = OnSurfaceText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(timeLabel, color = SubtleText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, null, tint = SubtleText, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    conn.domain,
                    color = SubtleText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${conn.ip}  •  ${conn.country}", color = SubtleText.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(8.dp))

        // Risk badge
        Surface(
            color = riskColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                riskLabel,
                color = riskColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = SubtleText, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
