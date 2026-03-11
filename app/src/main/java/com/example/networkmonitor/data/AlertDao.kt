package com.example.networkmonitor.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM alerts WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM alerts WHERE timestamp < :before")
    suspend fun deleteAlertsBefore(before: Long)

    @Query("SELECT COUNT(*) FROM alerts WHERE domain = :domain AND timestamp > :since")
    suspend fun countRecentAlertsForDomain(domain: String, since: Long): Int
}
