package com.example.networkmonitor.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY timestamp DESC")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getConnectionsForApp(packageName: String): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getConnectionsSince(since: Long): Flow<List<ConnectionEntity>>

    @Query("SELECT DISTINCT packageName, appName FROM connections ORDER BY appName ASC")
    fun getDistinctApps(): Flow<List<AppSummary>>

    @Query("""
        SELECT packageName, appName, COUNT(*) as connectionCount, MAX(timestamp) as lastSeen
        FROM connections
        GROUP BY packageName
        ORDER BY lastSeen DESC
    """)
    fun getAppSummaries(): Flow<List<AppSummary>>

    @Query("""
        SELECT DISTINCT domain, ip, country, MAX(timestamp) as lastSeen, COUNT(*) as connectionCount
        FROM connections
        WHERE packageName = :packageName
        GROUP BY domain
        ORDER BY lastSeen DESC
    """)
    fun getDomainsForApp(packageName: String): Flow<List<DomainSummary>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConnection(connection: ConnectionEntity): Long

    @Query("DELETE FROM connections WHERE timestamp < :before")
    suspend fun deleteConnectionsBefore(before: Long)

    @Query("SELECT COUNT(*) FROM connections")
    suspend fun getCount(): Int
}

data class AppSummary(
    val packageName: String,
    val appName: String,
    val connectionCount: Int = 0,
    val lastSeen: Long = 0
)

data class DomainSummary(
    val domain: String,
    val ip: String,
    val country: String,
    val lastSeen: Long,
    val connectionCount: Int
)
