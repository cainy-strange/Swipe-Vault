package com.example.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.AlbumItem
import com.example.data.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface DeleteRequestOutcome {
    data class RequiresConfirmation(val intentSender: IntentSender) : DeleteRequestOutcome
    data class CompletedDirectly(val deletedIds: List<Long>) : DeleteRequestOutcome
    data class Failed(val message: String) : DeleteRequestOutcome
}

class MediaStoreRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun loadAllPhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
        queryPhotos(bucketId = null)
    }

    suspend fun loadPhotosForAlbum(bucketId: Long): List<PhotoItem> = withContext(Dispatchers.IO) {
        if (bucketId == AlbumItem.ALL_PHOTOS_BUCKET_ID) {
            queryPhotos(bucketId = null)
        } else {
            queryPhotos(bucketId = bucketId)
        }
    }

    suspend fun loadAlbums(): List<AlbumItem> = withContext(Dispatchers.IO) {
        val photos = queryPhotos(bucketId = null)
        if (photos.isEmpty()) return@withContext emptyList()

        val grouped = photos.groupBy { it.bucketId }
        grouped.map { (bucketId, items) ->
            val newest = items.first() // already sorted newest-first
            AlbumItem(
                bucketId = bucketId,
                name = newest.bucketName.ifBlank { "Camera" },
                coverUri = newest.contentUri,
                photoCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                latestTimestamp = newest.dateTakenMillis
            )
        }.sortedByDescending { it.photoCount }
    }

    suspend fun findExistingMediaIds(mediaStoreIds: List<Long>): Set<Long> = withContext(Dispatchers.IO) {
        if (mediaStoreIds.isEmpty()) return@withContext emptySet()
        val existing = HashSet<Long>(mediaStoreIds.size)
        val chunkSize = 400
        val projection = arrayOf(MediaStore.Images.Media._ID)
        for (chunk in mediaStoreIds.chunked(chunkSize)) {
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${MediaStore.Images.Media._ID} IN ($placeholders)"
            val args = chunk.map { it.toString() }.toTypedArray()
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    existing.add(cursor.getLong(idCol))
                }
            }
        }
        existing
    }

    suspend fun requestDeletePhotos(uris: List<Uri>): DeleteRequestOutcome = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteRequestOutcome.Failed("No photos selected.")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                DeleteRequestOutcome.RequiresConfirmation(pendingIntent.intentSender)
            } else {
                val deletedIds = mutableListOf<Long>()
                for (uri in uris) {
                    try {
                        val rows = contentResolver.delete(uri, null, null)
                        if (rows > 0) {
                            ContentUris.parseId(uri).let { if (it > 0) deletedIds.add(it) }
                        }
                    } catch (securityException: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            securityException is RecoverableSecurityException
                        ) {
                            return@withContext DeleteRequestOutcome.RequiresConfirmation(
                                securityException.userAction.actionIntent.intentSender
                            )
                        }
                    }
                }
                DeleteRequestOutcome.CompletedDirectly(deletedIds)
            }
        } catch (e: Exception) {
            DeleteRequestOutcome.Failed(e.localizedMessage ?: "Unable to start OS delete request.")
        }
    }

    private fun queryPhotos(bucketId: Long?): List<PhotoItem> {
        val photos = ArrayList<PhotoItem>(256)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )

        val selection = if (bucketId != null) {
            "${MediaStore.Images.Media.BUCKET_ID} = ?"
        } else {
            null
        }
        val selectionArgs = if (bucketId != null) arrayOf(bucketId.toString()) else null
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val dateTaken = cursor.getLong(dateTakenCol)
                val dateAddedSeconds = cursor.getLong(dateAddedCol)
                val effectiveTimestamp = if (dateTaken > 0L) dateTaken else dateAddedSeconds * 1000L

                photos.add(
                    PhotoItem(
                        mediaStoreId = id,
                        contentUri = contentUri,
                        displayName = cursor.getString(nameCol) ?: "IMG_$id",
                        bucketId = cursor.getLong(bucketIdCol),
                        bucketName = cursor.getString(bucketNameCol) ?: "Gallery",
                        dateTakenMillis = effectiveTimestamp,
                        sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                        width = cursor.getInt(widthCol).coerceAtLeast(0),
                        height = cursor.getInt(heightCol).coerceAtLeast(0),
                        mimeType = cursor.getString(mimeCol) ?: "image/*"
                    )
                )
            }
        }

        // Strict chronological newest-first sort using normalized timestamps
        photos.sortByDescending { it.dateTakenMillis }
        return photos
    }
}
