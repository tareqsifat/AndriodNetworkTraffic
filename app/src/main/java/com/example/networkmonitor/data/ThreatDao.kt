package com.example.networkmonitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threats WHERE indicator = :indicator LIMIT 1")
    suspend fun findThreat(indicator: String): ThreatEntity?

    @Query("SELECT COUNT(*) FROM threats")
    fun getThreatCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM threats")
    suspend fun getThreatCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreats(threats: List<ThreatEntity>)

    @Query("DELETE FROM threats")
    suspend fun clearThreats()
}
