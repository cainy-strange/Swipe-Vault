package com.example.data.repository

import com.example.data.local.CleaningSessionEntity
import com.example.data.local.DeletionHistoryEntity
import com.example.data.local.SwipeDecisionEntity
import com.example.data.local.SwipeVaultDao
import com.example.data.media.DeleteRequestOutcome
import com.example.data.media.MediaStoreRepository
import com.example.data.media.StorageInspector
import com.example.data.model.AlbumItem
import com.example.data.model.CleaningStats
import com.example.data.model.PhotoItem
import com.example.data.model.StorageSnapshot
import com.example.data.model.SwipeDecisionType
import com.example.data.model.SwipeUndoAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class PreparedSwipeDeck(
    val totalInScope: Int,
    val unreviewedPhotos: List<PhotoItem>,
    val skippedPhotos: List<PhotoItem>,
    val keptCount: Int,
    val deleteQueueCount: Int,
    val skippedCount: Int
) {
    val reviewedCount: Int get() = keptCount + deleteQueueCount + skippedCount
}

data class ConfirmedDeletionResult(
    val confirmedDeletedCount: Int,
    val confirmedFreedBytes: Long,
    val updatedStorage: StorageSnapshot
)

data class GalleryOverview(
    val albums: List<AlbumItem>,
    val allPhotosSummary: AlbumItem,
    val storage: StorageSnapshot
)

class SwipeRepository(
    private val mediaStoreRepository: MediaStoreRepository,
    private val dao: SwipeVaultDao,
    private val storageInspector: StorageInspector
) {

    fun observeAllDecisions(): Flow<List<SwipeDecisionEntity>> = dao.observeAllDecisions()

    fun observeDeletionQueue(): Flow<List<SwipeDecisionEntity>> =
        dao.observeDecisionsByType(SwipeDecisionType.DELETE_QUEUE)

    fun observeSessions(): Flow<List<CleaningSessionEntity>> = dao.observeSessions()

    fun observeDeletionHistory(): Flow<List<DeletionHistoryEntity>> = dao.observeDeletionHistory()

    fun observeCleaningStats(): Flow<CleaningStats> {
        return combine(
            dao.observeAllDecisions(),
            dao.observeDeletionHistory(),
            dao.observeSessions()
        ) { decisions, history, sessions ->
            val keptCount = decisions.count { it.decision == SwipeDecisionType.KEEP }
            val queuedItems = decisions.filter { it.decision == SwipeDecisionType.DELETE_QUEUE }
            val skippedCount = decisions.count { it.decision == SwipeDecisionType.SKIP }
            val confirmedDeletedCount = history.size
            val actualRecoveredBytes = history.sumOf { it.sizeBytes }
            val totalReviewed = keptCount + queuedItems.size + skippedCount + confirmedDeletedCount
            val completedSessions = sessions.count { it.isCompleted }

            CleaningStats(
                totalReviewed = totalReviewed,
                totalKept = keptCount,
                totalQueuedForDeletion = queuedItems.size,
                pendingDeleteBytes = queuedItems.sumOf { it.sizeBytes },
                totalDeleted = confirmedDeletedCount,
                actualRecoveredBytes = actualRecoveredBytes,
                sessionsCompleted = completedSessions
            )
        }
    }

    fun getStorageSnapshot(): StorageSnapshot = storageInspector.getSnapshot()

    suspend fun loadGalleryOverview(): GalleryOverview = withContext(Dispatchers.IO) {
        val allPhotos = mediaStoreRepository.loadAllPhotos()
        val decisions = dao.getAllDecisionsOnce()
        val decidedIds = decisions
            .filter { it.decision == SwipeDecisionType.KEEP || it.decision == SwipeDecisionType.DELETE_QUEUE || it.decision == SwipeDecisionType.SKIP }
            .map { it.mediaStoreId }
            .toHashSet()

        val albums = allPhotos.groupBy { it.bucketId }.map { (bucketId, items) ->
            val newest = items.first()
            AlbumItem(
                bucketId = bucketId,
                name = newest.bucketName.ifBlank { "Camera" },
                coverUri = newest.contentUri,
                photoCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                latestTimestamp = newest.dateTakenMillis,
                reviewedCount = items.count { it.mediaStoreId in decidedIds }
            )
        }.sortedByDescending { it.photoCount }

        val allPhotosSummary = AlbumItem(
            bucketId = AlbumItem.ALL_PHOTOS_BUCKET_ID,
            name = "All Photos",
            coverUri = allPhotos.firstOrNull()?.contentUri,
            photoCount = allPhotos.size,
            totalSizeBytes = allPhotos.sumOf { it.sizeBytes },
            latestTimestamp = allPhotos.firstOrNull()?.dateTakenMillis ?: 0L,
            reviewedCount = allPhotos.count { it.mediaStoreId in decidedIds }
        )

        GalleryOverview(
            albums = albums,
            allPhotosSummary = allPhotosSummary,
            storage = storageInspector.getSnapshot()
        )
    }

    suspend fun prepareSwipeDeck(bucketId: Long): PreparedSwipeDeck = withContext(Dispatchers.IO) {
        val photos = mediaStoreRepository.loadPhotosForAlbum(bucketId)
        val decisionMap = dao.getAllDecisionsOnce().associateBy { it.mediaStoreId }

        val unreviewed = ArrayList<PhotoItem>(photos.size)
        val skipped = ArrayList<PhotoItem>()
        var keptCount = 0
        var deleteQueueCount = 0

        for (photo in photos) {
            when (decisionMap[photo.mediaStoreId]?.decision) {
                SwipeDecisionType.KEEP -> keptCount++
                SwipeDecisionType.DELETE_QUEUE -> deleteQueueCount++
                SwipeDecisionType.SKIP -> skipped.add(photo)
                null -> unreviewed.add(photo)
            }
        }

        PreparedSwipeDeck(
            totalInScope = photos.size,
            unreviewedPhotos = unreviewed,
            skippedPhotos = skipped,
            keptCount = keptCount,
            deleteQueueCount = deleteQueueCount,
            skippedCount = skipped.size
        )
    }

    suspend fun startOrResumeSession(
        bucketId: Long,
        albumName: String,
        totalPhotosInScope: Int
    ): Long = withContext(Dispatchers.IO) {
        val existing = dao.getActiveSessionForBucket(bucketId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.updateSession(
                existing.copy(
                    totalPhotosInScope = totalPhotosInScope,
                    updatedAtMillis = now
                )
            )
            return@withContext existing.id
        }
        dao.insertSession(
            CleaningSessionEntity(
                bucketId = bucketId,
                albumName = albumName,
                totalPhotosInScope = totalPhotosInScope,
                reviewedCount = 0,
                keptCount = 0,
                deletedQueueCount = 0,
                confirmedDeletedCount = 0,
                skippedCount = 0,
                recoveredBytes = 0L,
                startedAtMillis = now,
                updatedAtMillis = now,
                isCompleted = false
            )
        )
    }

    suspend fun recordSwipeDecision(
        photo: PhotoItem,
        decision: SwipeDecisionType,
        sessionId: Long,
        bucketIdScope: Long,
        totalInScope: Int
    ): SwipeDecisionType? = withContext(Dispatchers.IO) {
        val existing = dao.getDecisionByMediaId(photo.mediaStoreId)?.decision
        dao.upsertDecision(
            SwipeDecisionEntity(
                mediaStoreId = photo.mediaStoreId,
                contentUri = photo.contentUri.toString(),
                bucketId = photo.bucketId,
                bucketName = photo.bucketName,
                displayName = photo.displayName,
                sizeBytes = photo.sizeBytes,
                dateTakenMillis = photo.dateTakenMillis,
                decision = decision,
                updatedAtMillis = System.currentTimeMillis(),
                sessionId = sessionId
            )
        )
        updateSessionMetrics(sessionId, bucketIdScope, totalInScope)
        existing
    }

    suspend fun undoSwipeDecision(
        undoAction: SwipeUndoAction,
        sessionId: Long,
        bucketIdScope: Long,
        totalInScope: Int
    ) = withContext(Dispatchers.IO) {
        val prev = undoAction.previousDecision
        if (prev == null) {
            dao.deleteDecisionByMediaId(undoAction.photo.mediaStoreId)
        } else {
            dao.upsertDecision(
                SwipeDecisionEntity(
                    mediaStoreId = undoAction.photo.mediaStoreId,
                    contentUri = undoAction.photo.contentUri.toString(),
                    bucketId = undoAction.photo.bucketId,
                    bucketName = undoAction.photo.bucketName,
                    displayName = undoAction.photo.displayName,
                    sizeBytes = undoAction.photo.sizeBytes,
                    dateTakenMillis = undoAction.photo.dateTakenMillis,
                    decision = prev,
                    updatedAtMillis = System.currentTimeMillis(),
                    sessionId = sessionId
                )
            )
        }
        updateSessionMetrics(sessionId, bucketIdScope, totalInScope)
    }

    suspend fun removeFromDeletionQueue(mediaStoreId: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getDecisionByMediaId(mediaStoreId) ?: return@withContext
        dao.upsertDecision(
            existing.copy(
                decision = SwipeDecisionType.KEEP,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeMultipleFromDeletionQueue(mediaStoreIds: List<Long>) = withContext(Dispatchers.IO) {
        for (id in mediaStoreIds) {
            val existing = dao.getDecisionByMediaId(id)
            if (existing != null) {
                dao.upsertDecision(
                    existing.copy(
                        decision = SwipeDecisionType.KEEP,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun pruneMissingQueueItems(): Int = withContext(Dispatchers.IO) {
        val queued = dao.getAllDecisionsOnce()
            .filter { it.decision == SwipeDecisionType.DELETE_QUEUE }
        if (queued.isEmpty()) return@withContext 0
        val existingIds = mediaStoreRepository.findExistingMediaIds(queued.map { it.mediaStoreId })
        val missingIds = queued.map { it.mediaStoreId }.filter { it !in existingIds }
        if (missingIds.isNotEmpty()) {
            dao.deleteDecisionsByMediaIds(missingIds)
        }
        missingIds.size
    }

    suspend fun requestOsDeletion(
        items: List<SwipeDecisionEntity>
    ): Pair<Long, DeleteRequestOutcome> = withContext(Dispatchers.IO) {
        val storageBefore = storageInspector.getAvailableBytes()
        val uris = items.map { android.net.Uri.parse(it.contentUri) }
        val outcome = mediaStoreRepository.requestDeletePhotos(uris)
        storageBefore to outcome
    }

    suspend fun finalizeConfirmedDeletion(
        requestedItems: List<SwipeDecisionEntity>,
        storageBeforeBytes: Long
    ): ConfirmedDeletionResult = withContext(Dispatchers.IO) {
        val requestedIds = requestedItems.map { it.mediaStoreId }
        val stillExistingIds = mediaStoreRepository.findExistingMediaIds(requestedIds)
        val verifiedDeleted = requestedItems.filter { it.mediaStoreId !in stillExistingIds }

        // Recheck device storage after OS deletion completes
        val updatedStorage = storageInspector.getSnapshot()
        val now = System.currentTimeMillis()

        if (verifiedDeleted.isNotEmpty()) {
            val historyEntries = verifiedDeleted.map { item ->
                DeletionHistoryEntity(
                    batchId = now,
                    mediaStoreId = item.mediaStoreId,
                    displayName = item.displayName,
                    bucketName = item.bucketName,
                    sizeBytes = item.sizeBytes,
                    deletedAtMillis = now,
                    storageBeforeBytes = storageBeforeBytes,
                    storageAfterBytes = updatedStorage.availableBytes
                )
            }
            dao.insertDeletionHistory(historyEntries)
            dao.deleteDecisionsByMediaIds(verifiedDeleted.map { it.mediaStoreId })

            // Update affected sessions with confirmed deleted counts and freed bytes
            val bySession = verifiedDeleted.groupBy { it.sessionId }
            for ((sessionId, deletedInSession) in bySession) {
                val session = dao.getSessionById(sessionId) ?: continue
                val freedInSession = deletedInSession.sumOf { it.sizeBytes }
                dao.updateSession(
                    session.copy(
                        confirmedDeletedCount = session.confirmedDeletedCount + deletedInSession.size,
                        deletedQueueCount = (session.deletedQueueCount - deletedInSession.size).coerceAtLeast(0),
                        recoveredBytes = session.recoveredBytes + freedInSession,
                        updatedAtMillis = now
                    )
                )
            }
        }

        ConfirmedDeletionResult(
            confirmedDeletedCount = verifiedDeleted.size,
            confirmedFreedBytes = verifiedDeleted.sumOf { it.sizeBytes },
            updatedStorage = updatedStorage
        )
    }

    private suspend fun updateSessionMetrics(
        sessionId: Long,
        bucketIdScope: Long,
        totalInScope: Int
    ) {
        val session = dao.getSessionById(sessionId) ?: return
        val allDecisions = dao.getAllDecisionsOnce()
        val scoped = if (bucketIdScope == AlbumItem.ALL_PHOTOS_BUCKET_ID) {
            allDecisions
        } else {
            allDecisions.filter { it.bucketId == bucketIdScope }
        }
        val kept = scoped.count { it.decision == SwipeDecisionType.KEEP }
        val queued = scoped.count { it.decision == SwipeDecisionType.DELETE_QUEUE }
        val skipped = scoped.count { it.decision == SwipeDecisionType.SKIP }
        val reviewed = kept + queued + skipped + session.confirmedDeletedCount
        val isComplete = totalInScope > 0 && (kept + queued + session.confirmedDeletedCount) >= totalInScope
        val now = System.currentTimeMillis()

        dao.updateSession(
            session.copy(
                totalPhotosInScope = totalInScope,
                reviewedCount = reviewed.coerceAtMost(totalInScope),
                keptCount = kept,
                deletedQueueCount = queued,
                skippedCount = skipped,
                updatedAtMillis = now,
                isCompleted = isComplete,
                completedAtMillis = if (isComplete) (session.completedAtMillis ?: now) else null
            )
        )
    }
}
