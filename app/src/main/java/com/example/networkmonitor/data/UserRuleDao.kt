package com.example.networkmonitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRuleDao {
    @Query("SELECT * FROM user_rules")
    fun getAllRules(): Flow<List<UserRuleEntity>>

    @Query("SELECT * FROM user_rules")
    suspend fun getAllRulesSync(): List<UserRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: UserRuleEntity)

    @Query("DELETE FROM user_rules WHERE target = :target AND isDomain = :isDomain")
    suspend fun deleteRule(target: String, isDomain: Boolean)
}
