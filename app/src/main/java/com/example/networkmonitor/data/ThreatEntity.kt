package com.example.networkmonitor.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a known threat indicator (domain, IP, or URL).
 * Indexed heavily for fast lookups during active VPN traffic monitoring.
 */
@Entity(
    tableName = "threats",
    indices = [
        Index(value = ["indicator"], unique = true)
    ]
)
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicator: String,           // e.g. "malicious.com" or "1.2.3.4"
    val type: String,                // "domain", "ip", "url"
    val source: String,              // "PhishTank", "AbuseIPDB", "URLHaus", "GoogleSafeBrowsing"
    val riskLevel: Int,              // 0=SAFE, 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
    val category: String = "",       // e.g. "phishing", "botnet", "malware"
    val explanation: String = "",    // Human-readable reason
    val addedAt: Long = System.currentTimeMillis()
)
