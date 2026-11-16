package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CleaningSessionEntity
import com.example.data.local.DeletionHistoryEntity
import com.example.data.local.SwipeDecisionEntity
import com.example.data.local.SwipeVaultDatabase
import com.example.data.media.MediaStoreRepository
import com.example.data.media.StorageInspector
import com.example.data.model.PhotoItem
import com.example.data.model.SwipeDecisionType
import com.example.data.model.SwipeUndoAction
import com.example.data.repository.SwipeRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SwipeVaultCoreTest {

    private lateinit var database: SwipeVaultDatabase
    private lateinit var repository: SwipeRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SwipeVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SwipeRepository(
            mediaStoreRepository = MediaStoreRepository(context),
            dao = database.swipeVaultDao(),
            storageInspector = StorageInspector()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `keep, delete queue, skip, and undo persist accurately in Room`() = runBlocking {
        val dao = database.swipeVaultDao()
        val sessionId = dao.insertSession(
            CleaningSessionEntity(
                bucketId = 10L,
                albumName = "Camera",
                totalPhotosInScope = 3,
                reviewedCount = 0,
                keptCount = 0,
                deletedQueueCount = 0,
                confirmedDeletedCount = 0,
                skippedCount = 0,
                recoveredBytes = 0L,
                startedAtMillis = 1000L,
                updatedAtMillis = 1000L
            )
        )

        val photo1 = PhotoItem(
            mediaStoreId = 101L,
            contentUri = Uri.parse("content://media/external/images/media/101"),
            displayName = "IMG_101.jpg",
            bucketId = 10L,
            bucketName = "Camera",
            dateTakenMillis = 3000L,
            sizeBytes = 2_000_000L,
            width = 4000,
            height = 3000,
            mimeType = "image/jpeg"
        )
        val photo2 = photo1.copy(
            mediaStoreId = 102L,
            contentUri = Uri.parse("content://media/external/images/media/102"),
            displayName = "IMG_102.jpg",
            dateTakenMillis = 2000L,
            sizeBytes = 3_500_000L
        )

        val prev1 = repository.recordSwipeDecision(photo1, SwipeDecisionType.DELETE_QUEUE, sessionId, 10L, 3)
        assertNull(prev1)
        repository.recordSwipeDecision(photo2, SwipeDecisionType.KEEP, sessionId, 10L, 3)

        val queueBeforeUndo = repository.observeDeletionQueue().first()
        assertEquals(1, queueBeforeUndo.size)
        assertEquals(101L, queueBeforeUndo.first().mediaStoreId)

        // Pending queue must NOT be reported as actual recovered storage yet
        val statsBeforeDelete = repository.observeCleaningStats().first()
        assertEquals(0L, statsBeforeDelete.actualRecoveredBytes)
        assertEquals(2_000_000L, statsBeforeDelete.pendingDeleteBytes)

        // Undo the DELETE_QUEUE decision on photo1
        repository.undoSwipeDecision(
            undoAction = SwipeUndoAction(
                photo = photo1,
                previousDecision = null,
                appliedDecision = SwipeDecisionType.DELETE_QUEUE
            ),
            sessionId = sessionId,
            bucketIdScope = 10L,
            totalInScope = 3
        )

        val queueAfterUndo = repository.observeDeletionQueue().first()
        assertTrue(queueAfterUndo.isEmpty())
    }

    @Test
    fun `confirmed deletion reports actual recovered storage only after verification`() = runBlocking {
        val dao = database.swipeVaultDao()
        val sessionId = dao.insertSession(
            CleaningSessionEntity(
                bucketId = 20L,
                albumName = "Screenshots",
                totalPhotosInScope = 2,
                reviewedCount = 1,
                keptCount = 0,
                deletedQueueCount = 1,
                confirmedDeletedCount = 0,
                skippedCount = 0,
                recoveredBytes = 0L,
                startedAtMillis = 1000L,
                updatedAtMillis = 1000L
            )
        )

        val queuedItem = SwipeDecisionEntity(
            mediaStoreId = 999999L,
            contentUri = "content://media/external/images/media/999999",
            bucketId = 20L,
            bucketName = "Screenshots",
            displayName = "shot.png",
            sizeBytes = 4_200_000L,
            dateTakenMillis = 5000L,
            decision = SwipeDecisionType.DELETE_QUEUE,
            updatedAtMillis = 5500L,
            sessionId = sessionId
        )
        dao.upsertDecision(queuedItem)

        // Because 999999 does not exist in Robolectric MediaStore, finalizeConfirmedDeletion verifies removal and rechecks storage
        val result = repository.finalizeConfirmedDeletion(
            requestedItems = listOf(queuedItem),
            storageBeforeBytes = 10_000_000L
        )
        assertEquals(1, result.confirmedDeletedCount)
        assertEquals(4_200_000L, result.confirmedFreedBytes)

        val stats = repository.observeCleaningStats().first()
        assertEquals(1, stats.totalDeleted)
        assertEquals(4_200_000L, stats.actualRecoveredBytes)
        assertEquals(0, stats.totalQueuedForDeletion)
    }

    @Test
    fun `byte formatter renders human readable sizes`() {
        assertEquals("0 B", Formatters.formatBytes(0L))
        assertEquals("512 B", Formatters.formatBytes(512L))
        assertEquals("1.00 MB", Formatters.formatBytes(1024L * 1024L))
    }
}
