package com.example.networkmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConnectionEntity::class,
        AlertEntity::class,
        ThreatEntity::class,
        UserRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun alertDao(): AlertDao
    abstract fun threatDao(): ThreatDao
    abstract fun userRuleDao(): UserRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "network_monitor_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
