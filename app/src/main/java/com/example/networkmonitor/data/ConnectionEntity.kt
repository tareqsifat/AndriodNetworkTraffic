package com.example.networkmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val domain: String,
    val ip: String,
    val country: String,
    val organization: String = "",
    val riskLevel: Int = 0, // 0=Safe, 1=Suspicious, 2=Malicious
    val riskSource: String = "",
    val protocol: String = "TCP" // TCP, UDP, DNS
)
