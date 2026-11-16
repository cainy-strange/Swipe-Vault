package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.PhotoItem
import com.example.data.model.SwipeDecisionType
import com.example.ui.components.FullScreenPhotoModal
import com.example.ui.components.TinderSwipeDeck
import com.example.ui.components.rememberSwipeDeckController
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHigh
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.CrimsonDeleteContainer
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldKeepContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.viewmodel.SwipeUiState
import com.example.util.Formatters

@Composable
fun SwipeScreen(
    uiState: SwipeUiState,
    onBack: () -> Unit,
    onSwipeDecision: (SwipeDecisionType) -> Unit,
    onUndo: () -> Unit,
    onTapPhoto: (PhotoItem) -> Unit,
    onDismissFullScreen: () -> Unit,
    onRevisitSkipped: () -> Unit,
    onOpenDeletionQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val controller = rememberSwipeDeckController(onDecisionCommitted = onSwipeDecision)

    // Preload next 3 photos for zero-jank card transitions
    LaunchedEffect(uiState.preloadUris) {
        val loader = context.imageLoader
        uiState.preloadUris.forEach { uri ->
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(1200, 1600)
                .build()
            loader.enqueue(request)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top HUD Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CharcoalSurface)
                            .testTag("swipe_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                    Column {
                        Text(
                            text = uiState.albumName,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isSkippedMode) "Revisiting skipped photos" else "Newest first • Tap to zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGrey
                        )
                    }
                }

                // Live Queue Pill
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenDeletionQueue)
                        .testTag("swipe_open_queue_pill"),
                    shape = CircleShape,
                    color = if (uiState.deleteQueueCount > 0) CrimsonDeleteContainer else CharcoalSurface,
                    border = BorderStroke(
                        1.dp,
                        if (uiState.deleteQueueCount > 0) CrimsonDelete.copy(alpha = 0.6f) else HairlineBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Review deletion queue",
                            tint = if (uiState.deleteQueueCount > 0) CrimsonDelete else SilverText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${uiState.deleteQueueCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                    }
                }
            }

            // Progress Dashboard Strip
            SwipeProgressDashboard(uiState = uiState)

            // Main Card Deck or Loading / Completed State
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
                    }
                }

                uiState.currentPhoto == null -> {
                    SwipeSessionCompleteCard(
                        uiState = uiState,
                        onRevisitSkipped = onRevisitSkipped,
                        onOpenDeletionQueue = onOpenDeletionQueue,
                        onBack = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                else -> {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val widthPx = with(density) { maxWidth.toPx() }
                        val heightPx = with(density) { maxHeight.toPx() }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                TinderSwipeDeck(
                                    currentPhoto = uiState.currentPhoto!!,
                                    nextPhoto = uiState.nextPhoto,
                                    controller = controller,
                                    onSwipeCommitted = onSwipeDecision,
                                    onTapPhoto = onTapPhoto
                                )
                            }

                            // Bottom Tinder-style Controls: Undo | Delete | Skip | Keep
                            SwipeActionDock(
                                canUndo = uiState.canUndo,
                                onUndo = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUndo()
                                },
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    controller.programSwipe(SwipeDecisionType.DELETE_QUEUE, widthPx, heightPx)
                                },
                                onSkip = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    controller.programSwipe(SwipeDecisionType.SKIP, widthPx, heightPx)
                                },
                                onKeep = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    controller.programSwipe(SwipeDecisionType.KEEP, widthPx, heightPx)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Photo Lightbox with Pinch-to-Zoom
        val fullPhoto = uiState.fullScreenPhoto
        if (fullPhoto != null) {
            FullScreenPhotoModal(
                imageUri = fullPhoto.contentUri.toString(),
                title = fullPhoto.displayName,
                subtitle = "${Formatters.formatDate(fullPhoto.dateTakenMillis)} • ${Formatters.formatBytes(fullPhoto.sizeBytes)} • ${fullPhoto.width}×${fullPhoto.height}",
                onDismiss = onDismissFullScreen,
                onDelete = { onSwipeDecision(SwipeDecisionType.DELETE_QUEUE) },
                onSkip = { onSwipeDecision(SwipeDecisionType.SKIP) },
                onKeep = { onSwipeDecision(SwipeDecisionType.KEEP) }
            )
        }
    }
}

@Composable
private fun SwipeProgressDashboard(uiState: SwipeUiState) {
    val animatedProgress = animateFloatAsState(
        targetValue = uiState.progressFraction,
        animationSpec = tween(durationMillis = 280),
        label = "swipe_progress"
    ).value

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("swipe_progress_dashboard"),
        shape = MaterialTheme.shapes.medium,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reviewed: ${uiState.reviewedCount} / ${uiState.totalPhotosInScope}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite
                )
                Text(
                    text = "Progress: ${uiState.progressPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldKeep
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = PureWhite,
                trackColor = CharcoalHigh
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(label = "Kept", value = uiState.keptCount, color = EmeraldKeep)
                MetricPill(label = "Delete", value = uiState.deleteQueueCount, color = CrimsonDelete)
                MetricPill(label = "Skipped", value = uiState.skippedCount, color = SilverText)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            color = SilverText
        )
    }
}

@Composable
private fun SwipeActionDock(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    onSkip: () -> Unit,
    onKeep: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (canUndo) CharcoalElevated else CharcoalSurface)
                .border(
                    1.dp,
                    if (canUndo) PureWhite.copy(alpha = 0.28f) else HairlineBorder,
                    CircleShape
                )
                .testTag("swipe_undo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo last swipe",
                tint = if (canUndo) PureWhite else MutedGrey,
                modifier = Modifier.size(22.dp)
            )
        }

        // Delete
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CrimsonDeleteContainer)
                    .border(1.5.dp, CrimsonDelete, CircleShape)
                    .clickable(onClick = onDelete)
                    .testTag("swipe_delete_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Mark for deletion",
                    tint = CrimsonDelete,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text("Delete", style = MaterialTheme.typography.labelSmall, color = CrimsonDelete)
        }

        // Skip
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CharcoalElevated)
                    .border(1.dp, HairlineBorder, CircleShape)
                    .clickable(onClick = onSkip)
                    .testTag("swipe_skip_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Skip photo",
                    tint = SilverText,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text("Skip", style = MaterialTheme.typography.labelSmall, color = SilverText)
        }

        // Keep
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldKeepContainer)
                    .border(1.5.dp, EmeraldKeep, CircleShape)
                    .clickable(onClick = onKeep)
                    .testTag("swipe_keep_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Keep photo",
                    tint = EmeraldKeep,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text("Keep", style = MaterialTheme.typography.labelSmall, color = EmeraldKeep)
        }

        // Visual balance spacer matching Undo width
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun SwipeSessionCompleteCard(
    uiState: SwipeUiState,
    onRevisitSkipped: () -> Unit,
    onOpenDeletionQueue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = CharcoalSurface,
            border = BorderStroke(1.dp, HairlineBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vault_empty),
                    contentDescription = "Session complete",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Text(
                    text = if (uiState.totalPhotosInScope == 0) {
                        "No Photos in Album"
                    } else {
                        "Deck Complete"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (uiState.totalPhotosInScope == 0) {
                        "This album currently has no accessible images."
                    } else {
                        "You reviewed ${uiState.reviewedCount} of ${uiState.totalPhotosInScope} photos. Kept ${uiState.keptCount}, queued ${uiState.deleteQueueCount} for deletion."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverText,
                    textAlign = TextAlign.Center
                )

                if (uiState.skippedPool.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onRevisitSkipped,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("revisit_skipped_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Revisit ${uiState.skippedPool.size} Skipped Photo(s)",
                            color = PureWhite
                        )
                    }
                }

                if (uiState.deleteQueueCount > 0) {
                    Button(
                        onClick = onOpenDeletionQueue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("complete_review_queue_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonDelete,
                            contentColor = PureWhite
                        )
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Review Deletion Queue (${uiState.deleteQueueCount})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("complete_back_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Text("Back to Albums", color = SilverText)
                }
            }
        }
    }
}
