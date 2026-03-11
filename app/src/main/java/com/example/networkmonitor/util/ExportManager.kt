package com.example.networkmonitor.util

import android.content.Context
import android.net.Uri
import com.example.networkmonitor.data.ConnectionEntity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

object ExportManager {

    suspend fun exportJson(context: Context, connections: List<ConnectionEntity>, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val exportData = connections.map {
                    mapOf(
                        "timestamp" to it.timestamp,
                        "app_name" to it.appName,
                        "package_name" to it.packageName,
                        "domain" to it.domain,
                        "ip" to it.ip,
                        "country" to it.country,
                        "organization" to it.organization,
                        "risk_level" to riskLevelLabel(it.riskLevel),
                        "risk_source" to it.riskSource,
                        "protocol" to it.protocol
                    )
                }
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(gson.toJson(exportData))
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun exportCsv(context: Context, connections: List<ConnectionEntity>, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write("timestamp,app_name,package_name,domain,ip,country,organization,risk_level,risk_source,protocol\n")
                        connections.forEach { c ->
                            writer.write(
                                "${c.timestamp},\"${c.appName}\",\"${c.packageName}\"," +
                                        "\"${c.domain}\",${c.ip},\"${c.country}\"," +
                                        "\"${c.organization}\",${riskLevelLabel(c.riskLevel)}," +
                                        "\"${c.riskSource}\",${c.protocol}\n"
                            )
                        }
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun riskLevelLabel(riskLevel: Int) = when (riskLevel) {
        0 -> "Safe"
        1 -> "Low"
        2 -> "Medium"
        3 -> "High"
        4 -> "Critical"
        else -> "Unknown"
    }
}
