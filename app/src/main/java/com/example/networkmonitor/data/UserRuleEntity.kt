package com.example.networkmonitor.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user-defined action for a specific domain/IP.
 */
@Entity(
    tableName = "user_rules",
    indices = [
        Index(value = ["target", "isDomain"], unique = true)
    ]
)
data class UserRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val target: String,         // e.g. "suspicious.xyz" or "185.x.x.x"
    val isDomain: Boolean,
    val action: String,         // "BLOCK" or "IGNORE"
    val createdAt: Long = System.currentTimeMillis()
)
