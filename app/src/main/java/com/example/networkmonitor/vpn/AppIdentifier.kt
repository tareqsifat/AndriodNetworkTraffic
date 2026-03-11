package com.example.networkmonitor.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.example.networkmonitor.data.AppSummary

/**
 * Resolves the app (package name + display name) responsible for a given network connection.
 *
 * Android provides UID-to-package mapping via PackageManager.
 * We use ConnectivityManager to find the UID owning a connection by its ports.
 */
class AppIdentifier(private val context: Context) {

    private val packageManager = context.packageManager

    /**
     * Returns (packageName, appName) for the given ports.
     * Falls back to ("unknown", "Unknown App") if resolution fails.
     */
    fun resolveApp(srcPort: Int, dstPort: Int, protocol: String): Pair<String, String> {
        return try {
            // Read /proc/net/tcp or /proc/net/udp for UID resolution
            val uid = readUidFromProc(srcPort, protocol)
            if (uid < 0) return Pair("unknown", "Unknown App")

            val packages = packageManager.getPackagesForUid(uid)
            val packageName = packages?.firstOrNull() ?: "unknown"
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            Pair(packageName, appName)
        } catch (e: Exception) {
            Log.w("AppIdentifier", "Failed to resolve app: ${e.message}")
            Pair("unknown", "Unknown App")
        }
    }

    /**
     * Reads /proc/net/tcp or /proc/net/udp to find the UID owning a local port.
     * The hex-encoded port is matched against the local_address column.
     */
    private fun readUidFromProc(port: Int, protocol: String): Int {
        val file = when (protocol.uppercase()) {
            "TCP" -> "/proc/net/tcp"
            "UDP" -> "/proc/net/udp"
            else -> return -1
        }
        val portHex = port.toString(16).uppercase().padStart(4, '0')
        try {
            java.io.BufferedReader(java.io.FileReader(file)).use { reader ->
                reader.readLine() // skip header
                for (line in reader.lines()) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size < 8) continue
                    val localAddress = parts[1] // e.g. "0100007F:0050"
                    val localPort = localAddress.split(":").getOrNull(1) ?: continue
                    if (localPort.equals(portHex, ignoreCase = true)) {
                        return parts[7].toIntOrNull() ?: -1
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AppIdentifier", "Error reading $file: ${e.message}")
        }
        return -1
    }
}
