package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SwipeDecisionType

@Entity(tableName = "swipe_decisions")
data class SwipeDecisionEntity(
    @PrimaryKey val mediaStoreId: Long,
    val contentUri: String,
    val bucketId: Long,
    val bucketName: String,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val decision: SwipeDecisionType,
    val updatedAtMillis: Long,
    val sessionId: Long
)

@Entity(tableName = "cleaning_sessions")
data class CleaningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bucketId: Long,
    val albumName: String,
    val totalPhotosInScope: Int,
    val reviewedCount: Int,
    val keptCount: Int,
    val deletedQueueCount: Int,
    val confirmedDeletedCount: Int,
    val skippedCount: Int,
    val recoveredBytes: Long,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val isCompleted: Boolean = false
)

@Entity(tableName = "deletion_history")
data class DeletionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val batchId: Long,
    val mediaStoreId: Long,
    val displayName: String,
    val bucketName: String,
    val sizeBytes: Long,
    val deletedAtMillis: Long,
    val storageBeforeBytes: Long,
    val storageAfterBytes: Long
)
