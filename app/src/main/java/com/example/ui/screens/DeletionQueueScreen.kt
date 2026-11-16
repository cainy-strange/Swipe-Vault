package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.SwipeDecisionEntity
import com.example.ui.components.FullScreenPhotoModal
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
import com.example.ui.viewmodel.DeletionQueueUiState
import com.example.ui.viewmodel.DeletionUiEvent
import com.example.util.Formatters
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun DeletionQueueScreen(
    uiState: DeletionQueueUiState,
    events: SharedFlow<DeletionUiEvent>,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRemoveIndividual: (Long) -> Unit,
    onKeepSelected: () -> Unit,
    onOpenPreview: (SwipeDecisionEntity) -> Unit,
    onClosePreview: () -> Unit,
    onOpenConfirmDialog: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onConfirmOsDelete: () -> Unit,
    onOsDeleteResult: (Boolean) -> Unit,
    onDismissBanner: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    modifier: Modifier = Modifier
) {
    val osDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        onOsDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is DeletionUiEvent.LaunchIntentSender -> {
                    osDeleteLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is DeletionUiEvent.ShowMessage -> Unit
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Deletion Queue",
                        style = MaterialTheme.typography.displaySmall,
                        color = PureWhite
                    )
                    Text(
                        text = if (uiState.queuedPhotos.isEmpty()) {
                            "No photos waiting for deletion"
                        } else {
                            "${uiState.queuedPhotos.size} queued • ${Formatters.formatBytes(uiState.totalQueueBytes)} est. pending"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                }
            }

            // Status Banner (after confirmation / cancellation / restoration)
            if (uiState.statusBanner != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = EmeraldKeepContainer,
                    border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.statusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss status",
                                tint = PureWhite
                            )
                        }
                    }
                }
            }

            if (uiState.queuedPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                                contentDescription = "Deletion queue is empty",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Text(
                                text = "Queue Is Clear",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                            Text(
                                text = "Photos you swipe left on appear here first for safe review. Nothing is ever deleted immediately.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverText,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onNavigateToAlbums,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("queue_empty_browse_albums"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text("Browse Albums to Clean", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Select All / Deselect All + Restore Selected Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = if (uiState.allSelected) onDeselectAll else onSelectAll,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("queue_select_all_toggle"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SelectAll,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.allSelected) "Deselect All" else "Select All",
                            color = PureWhite
                        )
                    }

                    if (uiState.selectedIds.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onKeepSelected,
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("queue_keep_selected_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestoreFromTrash,
                                contentDescription = null,
                                tint = EmeraldKeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Keep (${uiState.selectedIds.size})",
                                color = EmeraldKeep
                            )
                        }
                    }
                }

                // Grid of Queued Photos
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(
                        items = uiState.queuedPhotos,
                        key = { it.mediaStoreId }
                    ) { item ->
                        val isSelected = item.mediaStoreId in uiState.selectedIds
                        DeletionQueueTile(
                            item = item,
                            isSelected = isSelected,
                            onToggleSelect = { onToggleSelect(item.mediaStoreId) },
                            onOpenPreview = { onOpenPreview(item) },
                            onRemoveFromQueue = { onRemoveIndividual(item.mediaStoreId) }
                        )
                    }
                }

                // Sticky Safe Deletion Confirmation Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = CharcoalElevated,
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = EmeraldKeep,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Android OS confirmation required • Cancel anytime to keep originals",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverText
                            )
                        }
                        Button(
                            onClick = onOpenConfirmDialog,
                            enabled = uiState.selectedIds.isNotEmpty() && !uiState.isDeleting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("queue_delete_confirm_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonDelete,
                                contentColor = PureWhite,
                                disabledContainerColor = CharcoalHigh,
                                disabledContentColor = MutedGrey
                            )
                        ) {
                            if (uiState.isDeleting) {
                                CircularProgressIndicator(
                                    color = PureWhite,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.selectedIds.isEmpty()) {
                                        "Select Photos to Delete"
                                    } else {
                                        "Delete ${uiState.selectedIds.size} Selected (${Formatters.formatBytes(uiState.selectedBytes)})"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Final Confirmation Dialog before launching native OS MediaStore.createDeleteRequest
        if (uiState.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = onDismissConfirmDialog,
                containerColor = CharcoalElevated,
                titleContentColor = PureWhite,
                textContentColor = SilverText,
                title = {
                    Text(
                        text = "Confirm Safe Deletion",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                text = {
                    Text(
                        text = "You are about to request permanent deletion of ${uiState.selectedIds.size} photo(s) (~${Formatters.formatBytes(uiState.selectedBytes)}). Android will display a system confirmation prompt. If you cancel, all original photos remain untouched."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmOsDelete,
                        modifier = Modifier.testTag("dialog_confirm_os_delete_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonDelete,
                            contentColor = PureWhite
                        )
                    ) {
                        Text("Continue to OS Prompt")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismissConfirmDialog,
                        modifier = Modifier.testTag("dialog_cancel_delete_button")
                    ) {
                        Text("Cancel", color = SilverText)
                    }
                }
            )
        }

        // Preview Modal
        val preview = uiState.previewPhoto
        if (preview != null) {
            FullScreenPhotoModal(
                imageUri = preview.contentUri,
                title = preview.displayName,
                subtitle = "${preview.bucketName} • ${Formatters.formatBytes(preview.sizeBytes)} • ${Formatters.formatDate(preview.dateTakenMillis)}",
                onDismiss = onClosePreview,
                onRestoreKeep = { onRemoveIndividual(preview.mediaStoreId) }
            )
        }
    }
}

@Composable
private fun DeletionQueueTile(
    item: SwipeDecisionEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpenPreview: () -> Unit,
    onRemoveFromQueue: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(CharcoalSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CrimsonDelete else HairlineBorder,
                shape = shape
            )
            .clickable(onClick = onOpenPreview)
            .testTag("queue_tile_${item.mediaStoreId}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.contentUri)
                .size(360, 360)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection checkbox in top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) CrimsonDelete else ObsidianBlack.copy(alpha = 0.72f))
                .border(1.dp, if (isSelected) CrimsonDelete else PureWhite.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onToggleSelect)
                .testTag("queue_select_${item.mediaStoreId}"),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected for deletion",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Individual Remove / Restore button in top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(ObsidianBlack.copy(alpha = 0.78f))
                .border(1.dp, EmeraldKeep.copy(alpha = 0.7f), CircleShape)
                .clickable(onClick = onRemoveFromQueue)
                .testTag("queue_remove_${item.mediaStoreId}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove from deletion queue",
                tint = EmeraldKeep,
                modifier = Modifier.size(16.dp)
            )
        }

        // Size tag bottom-left
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            shape = RoundedCornerShape(6.dp),
            color = ObsidianBlack.copy(alpha = 0.78f)
        ) {
            Text(
                text = Formatters.formatBytes(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
