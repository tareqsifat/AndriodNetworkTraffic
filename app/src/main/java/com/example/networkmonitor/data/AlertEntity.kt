package com.example.networkmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val ip: String,
    val packageName: String,
    val appName: String,
    val riskLevel: Int, // 1=Suspicious, 2=Malicious
    val riskSource: String,
    val description: String = "",
    val isRead: Boolean = false
)
