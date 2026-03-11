package com.example.networkmonitor.vpn

import android.content.Context
import android.util.Log
import com.example.networkmonitor.data.NetworkRepository
import com.example.networkmonitor.data.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simulates fetching a weekly threat intelligence dataset and flushing it to the local Room database.
 */
class ThreatUpdater(
    private val context: Context,
    private val repository: NetworkRepository
) {
    companion object {
        private const val PREF_NAME = "ThreatUpdatePrefs"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val UPDATE_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isUpdateNeeded(): Boolean {
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0)
        return (System.currentTimeMillis() - lastUpdated) > UPDATE_INTERVAL_MS
    }

    fun getLastUpdatedTimestamp(): Long = prefs.getLong(KEY_LAST_UPDATED, 0)

    suspend fun performUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i("ThreatUpdater", "Starting threat dataset download...")

            // For the scope of this project, we mock downloading a static dataset.
            // In a real app, this would be a bulk download from a CDN or API.
            val mockThreats = listOf(
                ThreatEntity(
                    indicator = "evil-tracker.xyz",
                    type = "domain",
                    source = "URLHaus",
                    riskLevel = 4, // CRITICAL
                    category = "malware",
                    explanation = "This domain is known to host malware payloads and trojans."
                ),
                ThreatEntity(
                    indicator = "phishing-login.net",
                    type = "domain",
                    source = "PhishTank",
                    riskLevel = 3, // HIGH
                    category = "phishing",
                    explanation = "Verified phishing URL aimed at stealing credentials."
                ),
                ThreatEntity(
                    indicator = "botnet-cc.ru",
                    type = "domain",
                    source = "AlienVault OTX",
                    riskLevel = 4, // CRITICAL
                    category = "botnet",
                    explanation = "Known botnet command and control (C&C) infrastructure."
                ),
                ThreatEntity(
                    indicator = "185.15.20.25",
                    type = "ip",
                    source = "AbuseIPDB",
                    riskLevel = 3, // HIGH
                    category = "spam",
                    explanation = "IP extensively reported for brute-force attacks and spam."
                )
            )

            // Replace old DB with new dataset
            repository.clearThreats()
            repository.insertThreats(mockThreats)

            // Update timestamp
            prefs.edit().putLong(KEY_LAST_UPDATED, System.currentTimeMillis()).apply()

            Log.i("ThreatUpdater", "Threat database updated successfully with ${mockThreats.size} records.")
            true
        } catch (e: Exception) {
            Log.e("ThreatUpdater", "Failed to update threat database", e)
            false
        }
    }
}
