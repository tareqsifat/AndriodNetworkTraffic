package com.example.networkmonitor.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.networkmonitor.R
import com.example.networkmonitor.data.AlertEntity
import com.example.networkmonitor.data.AppDatabase
import com.example.networkmonitor.data.ConnectionEntity
import com.example.networkmonitor.data.NetworkRepository
import com.example.networkmonitor.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class NetworkMonitorVpnService : VpnService() {

    companion object {
        private const val TAG = "NetworkMonitorVpn"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_service_channel"
        const val ACTION_START = "ACTION_START_VPN"
        const val ACTION_STOP = "ACTION_STOP_VPN"
        const val EXTRA_VPN_RUNNING = "vpn_running"

        @Volatile
        var isRunning = false
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var repository: NetworkRepository
    private lateinit var appIdentifier: AppIdentifier
    private lateinit var geoResolver: GeoIpResolver
    private lateinit var riskAnalyzer: RiskAnalyzer
    private lateinit var firewallManager: FirewallManager
    private lateinit var threatUpdater: ThreatUpdater

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = NetworkRepository(db.connectionDao(), db.alertDao())
        appIdentifier = AppIdentifier(applicationContext)
        geoResolver = GeoIpResolver()
        firewallManager = FirewallManager(repository)
        threatUpdater = ThreatUpdater(applicationContext, repository)
        riskAnalyzer = RiskAnalyzer(repository, firewallManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startVpn()
                START_STICKY
            }
        }
    }

    private fun startVpn() {
        if (isRunning) return
        try {
            vpnInterface = Builder()
                .setSession("NetworkMonitor")
                .addAddress("10.0.0.1", 32)
                .addRoute("0.0.0.0", 0)                      // capture all IPv4 traffic
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setMtu(1500)
                .establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
                return
            }

            isRunning = true
            Log.i(TAG, "VPN interface established")

            // Initialize Firewall Rules
            firewallManager.init(serviceScope)

            // Start packet capture loop
            serviceScope.launch { captureLoop() }

            // Prune old records periodically (keep 7 days)
            serviceScope.launch {
                while (isActive) {
                    delay(60 * 60 * 1000L) // every hour
                    repository.pruneOldConnections(7L * 24 * 60 * 60 * 1000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopVpn()
        }
    }

    private suspend fun captureLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteArray(32767)

        Log.i(TAG, "Packet capture loop started")
        while (isRunning && coroutineContext.isActive) {
            try {
                val length = withContext(Dispatchers.IO) { inputStream.read(packet) }
                if (length <= 0) continue

                // Quick pre-parse to check blocked domains/IPs
                val parsed = PacketParser.parse(packet, length)
                val dstIp = parsed?.dstIp
                val domain = parsed?.domain ?: dstIp

                // Firewall Block Check (Synchronous/In-Memory)
                if (parsed != null && domain != null && dstIp != null) {
                    if (firewallManager.isBlocked(domain, dstIp)) {
                        // DROP the packet: do not write to output stream
                        // Log blocked action asynchronously
                        serviceScope.launch {
                            logBlockedConnection(parsed, domain, dstIp)
                        }
                        continue
                    }
                }

                // Forward packet to internet FIRST to avoid adding latency
                withContext(Dispatchers.IO) { outputStream.write(packet, 0, length) }

                // Full parse and process asynchronously
                if (parsed != null) {
                    serviceScope.launch {
                        processPacket(parsed, domain ?: "", dstIp ?: "")
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Log.w(TAG, "Error reading packet: ${e.message}")
                break
            }
        }
        Log.i(TAG, "Packet capture loop ended")
    }

    private suspend fun processPacket(parsed: PacketParser.ParsedPacket, domain: String, dstIp: String) {
        // Resolve app from connection (by dst IP + port)
        val (packageName, appName) = appIdentifier.resolveApp(parsed.srcPort, parsed.dstPort, parsed.protocol)

        // Geo resolve
        val geoInfo = geoResolver.resolve(dstIp)

        // Risk analysis (now uses ThreatDao via RiskAnalyzer)
        val riskResult = riskAnalyzer.analyze(domain, dstIp)

        if (riskResult.level == -1) return // Dropped from internal logic

        val connection = ConnectionEntity(
            packageName = packageName,
            appName = appName,
            domain = domain,
            ip = dstIp,
            country = geoInfo.country,
            organization = geoInfo.organization,
            riskLevel = riskResult.level,
            riskSource = riskResult.source,
            protocol = parsed.protocol
        )
        repository.insertConnection(connection)

        // Create alert if suspicious or malicious (and not ignored by user)
        if (riskResult.level >= 2 && !firewallManager.isIgnored(domain)) { // 2=MED, 3=HIGH, 4=CRIT
            val alreadyAlerted = repository.hasRecentAlert(domain, windowMs = 5 * 60 * 1000)
            if (!alreadyAlerted) {
                repository.insertAlert(
                    AlertEntity(
                        domain = domain,
                        ip = dstIp,
                        packageName = packageName,
                        appName = appName,
                        riskLevel = riskResult.level,
                        riskSource = riskResult.source,
                        description = riskResult.description
                    )
                )
                showAlertNotification(appName, domain, riskResult.level)
            }
        }
    }

    private suspend fun logBlockedConnection(parsed: PacketParser.ParsedPacket, domain: String, dstIp: String) {
        val (packageName, appName) = appIdentifier.resolveApp(parsed.srcPort, parsed.dstPort, parsed.protocol)
        val connection = ConnectionEntity(
            packageName = packageName,
            appName = appName,
            domain = domain,
            ip = dstIp,
            country = "Blocked",
            organization = "Firewall Rule",
            riskLevel = 4, // Critical indicator
            riskSource = "User Blocklist",
            protocol = parsed.protocol
        )
        repository.insertConnection(connection)
    }

    private fun showAlertNotification(appName: String, domain: String, riskLevel: Int) {
        val label = when {
            riskLevel == 4 -> "🛑 Blocked by Policy:"
            riskLevel >= 3 -> "⛔ High Risk Domain:"
            else -> "⚠ Suspicious Domain:"
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(label)
            .setContentText("$appName → $domain")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun stopVpn() {
        isRunning = false
        serviceJob.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Network Monitor VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows when the network monitor is active" }
        nm.createNotificationChannel(channel)

        val stopIntent = Intent(this, NetworkMonitorVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Network Monitor Active")
            .setContentText("Monitoring network traffic...")
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
