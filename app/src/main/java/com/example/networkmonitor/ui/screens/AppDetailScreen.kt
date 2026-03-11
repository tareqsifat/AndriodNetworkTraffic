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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networkmonitor.data.DomainSummary
import com.example.networkmonitor.ui.theme.*
import com.example.networkmonitor.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: MainViewModel,
    packageName: String,
    onBack: () -> Unit
) {
    val domainsFlow = remember(packageName) { viewModel.getDomainsForApp(packageName) }
    val domains by domainsFlow.collectAsState(initial = emptyList())
    val appSummaries by viewModel.appSummaries.collectAsState()
    val appInfo = appSummaries.find { it.packageName == packageName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        appInfo?.appName ?: packageName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = OnSurfaceText
                    )
                    Text(packageName, color = SubtleText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = OnSurfaceText)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
        )

        // Summary cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                icon = Icons.Default.Hub,
                label = "Connections",
                value = "${appInfo?.connectionCount ?: 0}",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                icon = Icons.Default.Language,
                label = "Domains",
                value = "${domains.size}",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                icon = Icons.Default.Schedule,
                label = "Last Seen",
                value = appInfo?.lastSeen?.let { formatRelativeTime(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
        }

        // Domains section
        Text(
            "Servers Contacted",
            color = SubtleText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(domains, key = { it.domain }) { domain ->
                DomainRow(domain)
                Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
            }

            if (domains.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No domain data yet", color = SubtleText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = CyberGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = OnSurfaceText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = SubtleText, fontSize = 10.sp)
        }
    }
}

@Composable
fun DomainRow(domain: DomainSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = CyberGreen.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Dns, null, tint = CyberGreen, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(domain.domain, color = OnSurfaceText, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${domain.ip}  •  ${domain.country}",
                color = SubtleText,
                fontSize = 12.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${domain.connectionCount}×",
                color = CyberGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                formatRelativeTime(domain.lastSeen),
                color = SubtleText,
                fontSize = 10.sp
            )
        }
    }
}

fun formatRelativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "${diff / 1000}s ago"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}
