package com.example

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.SwipeDecisionEntity
import com.example.data.model.CleaningStats
import com.example.data.model.MediaPermissionState
import com.example.data.model.PhotoItem
import com.example.data.model.StorageSnapshot
import com.example.data.model.SwipeDecisionType
import com.example.ui.screens.DeletionQueueScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SwipeScreen
import com.example.ui.theme.SwipeVaultTheme
import com.example.ui.viewmodel.DeletionQueueUiState
import com.example.ui.viewmodel.DeletionUiEvent
import com.example.ui.viewmodel.GalleryUiState
import com.example.ui.viewmodel.SwipeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SwipeVaultUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home screen shows permission CTA when gallery access is denied`() {
        composeRule.setContent {
            SwipeVaultTheme {
                HomeScreen(
                    galleryState = GalleryUiState(
                        permissionState = MediaPermissionState.DENIED,
                        storageSnapshot = StorageSnapshot(totalBytes = 64_000_000_000L, availableBytes = 20_000_000_000L)
                    ),
                    stats = CleaningStats(),
                    recentSessions = emptyList(),
                    onRequestGalleryPermission = {},
                    onSelectMorePhotos = {},
                    onStartAllPhotosCleaning = {},
                    onSelectAlbum = {},
                    onOpenAlbums = {},
                    onOpenDeletionQueue = {},
                    onOpenStats = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SwipeVault").assertIsDisplayed()
        composeRule.onNodeWithTag("grant_gallery_access_button").assertIsDisplayed()
    }

    @Test
    fun `swipe screen displays progress dashboard and action buttons`() {
        val samplePhoto = PhotoItem(
            mediaStoreId = 1L,
            contentUri = Uri.parse("content://media/external/images/media/1"),
            displayName = "IMG_TEST.jpg",
            bucketId = 10L,
            bucketName = "Camera",
            dateTakenMillis = 1700000000000L,
            sizeBytes = 2_400_000L,
            width = 4000,
            height = 3000,
            mimeType = "image/jpeg"
        )
        var lastDecision: SwipeDecisionType? = null

        composeRule.setContent {
            SwipeVaultTheme {
                SwipeScreen(
                    uiState = SwipeUiState(
                        bucketId = 10L,
                        albumName = "Camera",
                        isLoading = false,
                        totalPhotosInScope = 10,
                        keptCount = 2,
                        deleteQueueCount = 1,
                        skippedCount = 1,
                        deck = listOf(samplePhoto),
                        canUndo = true
                    ),
                    onBack = {},
                    onSwipeDecision = { lastDecision = it },
                    onUndo = {},
                    onTapPhoto = {},
                    onDismissFullScreen = {},
                    onRevisitSkipped = {},
                    onOpenDeletionQueue = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("swipe_progress_dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("Reviewed: 4 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("Progress: 40%").assertIsDisplayed()
        composeRule.onNodeWithTag("swipe_delete_button").assertIsDisplayed()
        composeRule.onNodeWithTag("swipe_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("swipe_keep_button").assertIsDisplayed()
        composeRule.onNodeWithTag("swipe_undo_button").assertIsDisplayed()

        composeRule.onNodeWithTag("swipe_keep_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
        assertEquals(SwipeDecisionType.KEEP, lastDecision)
    }

    @Test
    fun `deletion queue screen renders queued photo and confirm dialog`() {
        val queued = SwipeDecisionEntity(
            mediaStoreId = 55L,
            contentUri = "content://media/external/images/media/55",
            bucketId = 10L,
            bucketName = "Camera",
            displayName = "IMG_055.jpg",
            sizeBytes = 3_145_728L,
            dateTakenMillis = 1700000000000L,
            decision = SwipeDecisionType.DELETE_QUEUE,
            updatedAtMillis = 1700000000000L,
            sessionId = 1L
        )
        var confirmClicked = false

        composeRule.setContent {
            SwipeVaultTheme {
                DeletionQueueScreen(
                    uiState = DeletionQueueUiState(
                        queuedPhotos = listOf(queued),
                        selectedIds = setOf(55L)
                    ),
                    events = MutableSharedFlow<DeletionUiEvent>(),
                    onToggleSelect = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onRemoveIndividual = {},
                    onKeepSelected = {},
                    onOpenPreview = {},
                    onClosePreview = {},
                    onOpenConfirmDialog = { confirmClicked = true },
                    onDismissConfirmDialog = {},
                    onConfirmOsDelete = {},
                    onOsDeleteResult = {},
                    onDismissBanner = {},
                    onNavigateToAlbums = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("queue_tile_55").assertIsDisplayed()
        composeRule.onNodeWithTag("queue_delete_confirm_button").performClick()
        assertEquals(true, confirmClicked)
    }
}
