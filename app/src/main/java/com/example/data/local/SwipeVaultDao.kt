package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SwipeDecisionType
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeVaultDao {

    @Query("SELECT * FROM swipe_decisions ORDER BY updatedAtMillis DESC")
    fun observeAllDecisions(): Flow<List<SwipeDecisionEntity>>

    @Query("SELECT * FROM swipe_decisions WHERE decision = :decision ORDER BY dateTakenMillis DESC")
    fun observeDecisionsByType(decision: SwipeDecisionType): Flow<List<SwipeDecisionEntity>>

    @Query("SELECT * FROM swipe_decisions")
    suspend fun getAllDecisionsOnce(): List<SwipeDecisionEntity>

    @Query("SELECT * FROM swipe_decisions WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getDecisionByMediaId(mediaStoreId: Long): SwipeDecisionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecision(entity: SwipeDecisionEntity)

    @Query("DELETE FROM swipe_decisions WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteDecisionByMediaId(mediaStoreId: Long)

    @Query("DELETE FROM swipe_decisions WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteDecisionsByMediaIds(mediaStoreIds: List<Long>)

    @Query("SELECT * FROM cleaning_sessions ORDER BY updatedAtMillis DESC")
    fun observeSessions(): Flow<List<CleaningSessionEntity>>

    @Query("SELECT * FROM cleaning_sessions WHERE bucketId = :bucketId AND isCompleted = 0 ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getActiveSessionForBucket(bucketId: Long): CleaningSessionEntity?

    @Query("SELECT * FROM cleaning_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): CleaningSessionEntity?

    @Insert
    suspend fun insertSession(session: CleaningSessionEntity): Long

    @Update
    suspend fun updateSession(session: CleaningSessionEntity)

    @Query("SELECT * FROM deletion_history ORDER BY deletedAtMillis DESC")
    fun observeDeletionHistory(): Flow<List<DeletionHistoryEntity>>

    @Insert
    suspend fun insertDeletionHistory(entries: List<DeletionHistoryEntity>)
}
