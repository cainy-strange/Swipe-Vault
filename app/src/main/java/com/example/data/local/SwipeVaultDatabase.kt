package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.SwipeDecisionType

class SwipeVaultTypeConverters {
    @TypeConverter
    fun fromDecisionType(value: SwipeDecisionType): String = value.name

    @TypeConverter
    fun toDecisionType(value: String): SwipeDecisionType =
        runCatching { SwipeDecisionType.valueOf(value) }.getOrDefault(SwipeDecisionType.SKIP)
}

@Database(
    entities = [
        SwipeDecisionEntity::class,
        CleaningSessionEntity::class,
        DeletionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SwipeVaultTypeConverters::class)
abstract class SwipeVaultDatabase : RoomDatabase() {
    abstract fun swipeVaultDao(): SwipeVaultDao

    companion object {
        @Volatile
        private var INSTANCE: SwipeVaultDatabase? = null

        fun getInstance(context: Context): SwipeVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SwipeVaultDatabase::class.java,
                    "swipevault_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
