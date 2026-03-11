package com.example.networkmonitor.vpn

import android.util.Log
import com.example.networkmonitor.data.NetworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Analyzes risk level of a domain/IP using the local Threat Intelligence Database
 * and optional AbuseIPDB API checks.
 *
 * Risk levels:
 *  0 = Safe
 *  1 = Low (Tracker/Analytics)
 *  2 = Medium (Suspicious TLD)
 *  3 = High (Phishing/Spam)
 *  4 = Critical (Malware/Botnet)
 */
class RiskAnalyzer(
    private val repository: NetworkRepository,
    private val firewallManager: FirewallManager
) {

    data class RiskResult(
        val level: Int,
        val source: String = "",
        val description: String = ""
    )

    // Known suspicious TLDs (heuristic)
    private val suspiciousTlds = setOf(
        ".xyz", ".top", ".club", ".work", ".loan", ".date", ".win", ".gq", ".cf", ".tk", ".ml", ".ga"
    )

    // Known tracking / analytics subdomains (low risk, informational)
    private val trackingKeywords = listOf(
        "analytics", "telemetry", "tracking", "metrics", "beacon", "pixel", "stat.",
        "stats.", "log.", "logs.", "track.", "event."
    )

    // Cache: domain|ip -> RiskResult
    private val cache = object : LinkedHashMap<String, RiskResult>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, RiskResult>): Boolean = size > 1000
    }

    suspend fun analyze(domain: String, ip: String): RiskResult {
        // First check firewall rules: if it's explicitly allowed/ignored, do not alert again
        if (firewallManager.isIgnored(domain)) {
            return RiskResult(level = 0, source = "User Rule", description = "Domain is ignored")
        }

        val key = "$domain|$ip"
        cache[key]?.let { return it }

        val result = doAnalyze(domain, ip)
        cache[key] = result
        return result
    }

    private suspend fun doAnalyze(domain: String, ip: String): RiskResult {
        // 1. Check local Threat Intelligence Database (Domains & IPs)
        val domainThreat = repository.checkThreat(domain)
        if (domainThreat != null) {
            return RiskResult(
                level = domainThreat.riskLevel,
                source = domainThreat.source,
                description = domainThreat.explanation.ifEmpty { "Known malicious domain." }
            )
        }

        val ipThreat = repository.checkThreat(ip)
        if (ipThreat != null) {
            return RiskResult(
                level = ipThreat.riskLevel,
                source = ipThreat.source,
                description = ipThreat.explanation.ifEmpty { "Known malicious IP address." }
            )
        }

        // 2. Suspicious TLD check -> MEDIUM Risk
        val hasSuspiciousTld = suspiciousTlds.any { domain.endsWith(it) }
        if (hasSuspiciousTld) {
            return RiskResult(
                level = 2,
                source = "TLD Heuristic",
                description = "Domain uses a high-risk TLD commonly associated with malware."
            )
        }

        // 3. Tracking keyword check -> LOW Risk (informational, no alert popups)
        val isTracker = trackingKeywords.any { domain.contains(it, ignoreCase = true) }
        if (isTracker) {
            return RiskResult(
                level = 1,
                source = "Tracker Heuristic",
                description = "Domain appears to be a tracking or analytics endpoint."
            )
        }

        // 4. AbuseIPDB check (async, requires API key in settings)
        val abuseResult = checkAbuseIPDB(ip)
        if (abuseResult != null) return abuseResult

        return RiskResult(level = 0, source = "Clean")
    }

    private suspend fun checkAbuseIPDB(ip: String): RiskResult? {
        val apiKey = getStoredApiKey() ?: return null
        if (ip == "unknown" || ip.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.abuseipdb.com/api/v2/check?ipAddress=$ip&maxAgeInDays=90")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Key", apiKey)
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    connection.disconnect()
                    parseAbuseIpDbResponse(response)
                } else {
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                Log.w("RiskAnalyzer", "AbuseIPDB check failed for $ip: ${e.message}")
                null
            }
        }
    }

    private fun parseAbuseIpDbResponse(json: String): RiskResult? {
        val scoreMatch = Regex(""""abuseConfidenceScore"\s*:\s*(\d+)""").find(json)
        val score = scoreMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return when {
            score >= 75 -> RiskResult(level = 4, source = "AbuseIPDB", description = "High abuse confidence score: $score%. Known malicious IP.")
            score >= 25 -> RiskResult(level = 3, source = "AbuseIPDB", description = "Moderate abuse confidence score: $score%. Proceed with caution.")
            else -> null
        }
    }

    private fun getStoredApiKey(): String? = null
}
