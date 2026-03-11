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
fun HistoryScreen(viewModel: MainViewModel) {
    val connections by viewModel.connections.collectAsState()

    // Group connections by date
    val grouped = remember(connections) {
        connections.groupBy { conn ->
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(conn.timestamp))
        }
    }

    var selectedFilter by remember { mutableStateOf(0) } // 0=All, 1=Suspicious, 2=Malicious
    val filters = listOf("All", "Suspicious", "Malicious")

    val filteredGrouped = remember(grouped, selectedFilter) {
        if (selectedFilter == 0) grouped
        else grouped.mapValues { (_, list) ->
            list.filter { it.riskLevel == selectedFilter }
        }.filter { it.value.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = CyberGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connection History", fontWeight = FontWeight.Bold, color = OnSurfaceText)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEachIndexed { idx, label ->
                val filterColor = when (idx) {
                    1 -> WarnAmber
                    2 -> DangerRed
                    else -> CyberGreen
                }
                FilterChip(
                    selected = selectedFilter == idx,
                    onClick = { selectedFilter = idx },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = filterColor.copy(alpha = 0.15f),
                        selectedLabelColor = filterColor,
                        containerColor = SurfaceElevated,
                        labelColor = SubtleText
                    )
                )
            }
        }

        Divider(color = BorderColor, thickness = 1.dp)

        if (filteredGrouped.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = SubtleText, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No history yet", color = SubtleText, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                filteredGrouped.forEach { (date, conns) ->
                    item(key = "header_$date") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceElevated)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = SubtleText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(date, color = SubtleText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("${conns.size} events", color = SubtleText.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    items(conns, key = { it.id }) { conn ->
                        HistoryRow(conn)
                        Divider(color = BorderColor.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(conn: ConnectionEntity) {
    val riskColor = when (conn.riskLevel) {
        2 -> DangerRed
        1 -> WarnAmber
        else -> SafeGreen
    }
    val timeStr = remember(conn.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(conn.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Protocol tag
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                conn.protocol,
                color = CyberGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${conn.appName} → ${conn.domain}",
                color = OnSurfaceText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${conn.ip}  •  ${conn.country}",
                color = SubtleText,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(timeStr, color = SubtleText, fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                ExportManager.riskLevelLabel(conn.riskLevel),
                color = riskColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
