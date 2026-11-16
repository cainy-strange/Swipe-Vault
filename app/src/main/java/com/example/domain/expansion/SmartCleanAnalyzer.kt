package com.example.domain.expansion

import com.example.data.model.PhotoItem

/**
 * Extensible local-only photo intelligence contract designed for Stage 6+ future modules:
 * - Duplicate & perceptual-similar photo detection
 * - Blurry photo detection
 * - Screenshot / WhatsApp media / large video / old photo cleaners
 * - Smart local cleaning recommendations
 *
 * All implementations must execute strictly on-device without uploading user media.
 */
enum class SmartCategory {
    DUPLICATES,
    SIMILAR_SHOTS,
    BLURRY_PHOTOS,
    SCREENSHOTS,
    LARGE_VIDEOS,
    WHATSAPP_MEDIA,
    OLD_MEMORIES
}

data class SmartCleanSuggestion(
    val category: SmartCategory,
    val title: String,
    val matchingMediaIds: List<Long>,
    val estimatedBytes: Long
)

interface SmartCleanAnalyzer {
    suspend fun analyze(photos: List<PhotoItem>): List<SmartCleanSuggestion>
}
