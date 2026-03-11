package com.example.networkmonitor.vpn

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Resolves IP address metadata (country, organization, ASN).
 *
 * Uses ip-api.com free tier for geo resolution (no API key needed, 45 req/min limit).
 * Falls back gracefully when offline or rate-limited.
 */
class GeoIpResolver {

    data class GeoInfo(
        val country: String = "Unknown",
        val countryCode: String = "?",
        val organization: String = "Unknown",
        val asn: String = ""
    )

    // Simple LRU-like cache to avoid repeated lookups
    private val cache = object : LinkedHashMap<String, GeoInfo>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, GeoInfo>): Boolean = size > 500
    }

    suspend fun resolve(ip: String): GeoInfo {
        // Don't resolve private/loopback IPs
        if (isPrivateIp(ip) || ip == "unknown") {
            return GeoInfo(country = "Local", countryCode = "LO", organization = "Local Network")
        }

        cache[ip]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("http://ip-api.com/json/$ip?fields=country,countryCode,org,as")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val geo = parseGeoResponse(response)
                cache[ip] = geo
                geo
            } catch (e: Exception) {
                Log.w("GeoIpResolver", "Failed to resolve $ip: ${e.message}")
                GeoInfo()
            }
        }
    }

    private fun parseGeoResponse(json: String): GeoInfo {
        // Simple manual parsing to avoid needing a JSON library (Gson already in app but keeping light)
        fun extractField(key: String): String {
            val pattern = Regex(""""$key"\s*:\s*"([^"]*)"|\s*"$key"\s*:\s*null""")
            return pattern.find(json)?.groupValues?.getOrNull(1) ?: ""
        }
        return GeoInfo(
            country = extractField("country").ifEmpty { "Unknown" },
            countryCode = extractField("countryCode").ifEmpty { "?" },
            organization = extractField("org").ifEmpty { "Unknown" },
            asn = extractField("as")
        )
    }

    private fun isPrivateIp(ip: String): Boolean {
        return try {
            val addr = InetAddress.getByName(ip)
            addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress
        } catch (e: Exception) {
            false
        }
    }
}
