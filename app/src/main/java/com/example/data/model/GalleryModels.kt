package com.example.data.model

import android.net.Uri

data class PhotoItem(
    val mediaStoreId: Long,
    val contentUri: Uri,
    val displayName: String,
    val bucketId: Long,
    val bucketName: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)

data class AlbumItem(
    val bucketId: Long,
    val name: String,
    val coverUri: Uri?,
    val photoCount: Int,
    val totalSizeBytes: Long,
    val latestTimestamp: Long,
    val reviewedCount: Int = 0
) {
    companion object {
        const val ALL_PHOTOS_BUCKET_ID = -1L
    }
}

enum class SwipeDecisionType {
    KEEP,
    DELETE_QUEUE,
    SKIP
}

data class SwipeUndoAction(
    val photo: PhotoItem,
    val previousDecision: SwipeDecisionType?,
    val appliedDecision: SwipeDecisionType
)

data class StorageSnapshot(
    val totalBytes: Long,
    val availableBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class CleaningStats(
    val totalReviewed: Int = 0,
    val totalKept: Int = 0,
    val totalQueuedForDeletion: Int = 0,
    val pendingDeleteBytes: Long = 0L,
    val totalDeleted: Int = 0,
    val actualRecoveredBytes: Long = 0L,
    val sessionsCompleted: Int = 0
)

enum class MediaPermissionState {
    FULL_ACCESS,
    PARTIAL_ACCESS,
    DENIED
}

enum class AlbumSortOption(val label: String) {
    PHOTO_COUNT("Most Photos"),
    STORAGE_SIZE("Largest Size"),
    RECENT("Most Recent"),
    NAME("Name A–Z")
}
