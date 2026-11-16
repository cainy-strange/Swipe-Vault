package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaPermissionState
import com.example.data.model.StorageSnapshot
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHigh
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.util.Formatters

@Composable
fun SwipeVaultLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = HairlineBorder,
                topLeft = Offset(w * 0.14f, h * 0.08f),
                size = Size(w * 0.68f, h * 0.78f),
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                style = Stroke(width = w * 0.07f)
            )
            drawRoundRect(
                color = PureWhite,
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = Size(w * 0.68f, h * 0.74f),
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                style = Stroke(width = w * 0.08f)
            )
            drawLine(
                color = EmeraldKeep,
                start = Offset(w * 0.38f, h * 0.56f),
                end = Offset(w * 0.72f, h * 0.38f),
                strokeWidth = w * 0.09f
            )
            drawCircle(
                color = CrimsonDelete,
                radius = w * 0.07f,
                center = Offset(w * 0.40f, h * 0.72f)
            )
        }
        Text(
            text = "SwipeVault",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite
        )
    }
}

@Composable
fun StorageMeterBar(
    storage: StorageSnapshot,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = storage.usedFraction,
        animationSpec = tween(durationMillis = 500),
        label = "storage_bar"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DEVICE STORAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MutedGrey
            )
            Text(
                text = "${Formatters.formatBytes(storage.availableBytes)} free of ${Formatters.formatBytes(storage.totalBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = SilverText
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(CharcoalHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PureWhite.copy(alpha = 0.85f), SilverText)
                        )
                    )
            )
        }
    }
}

@Composable
fun PermissionOnboardingCard(
    permissionState: MediaPermissionState,
    onRequestFullOrSelected: () -> Unit,
    onManageSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CharcoalHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = "Photo library permission",
                        tint = PureWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (permissionState == MediaPermissionState.PARTIAL_ACCESS) {
                            "Selected Photos Access Active"
                        } else {
                            "Connect Your Photo Library"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                    Text(
                        text = if (permissionState == MediaPermissionState.PARTIAL_ACCESS) {
                            "You granted access to selected photos. Add more photos or grant full access anytime."
                        } else {
                            "SwipeVault needs permission to index albums locally. Photos never leave your device."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = EmeraldKeep,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "100% local processing • OS-confirmed deletion only",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldKeep
                )
            }

            if (permissionState == MediaPermissionState.PARTIAL_ACCESS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onManageSelected,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("select_more_photos_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text("Select More", color = PureWhite)
                    }
                    Button(
                        onClick = onRequestFullOrSelected,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("grant_full_access_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("Full Access", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = onRequestFullOrSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_gallery_access_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Text("Grant Gallery Access", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoModal(
    imageUri: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onKeep: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRestoreKeep: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
            scale = newScale
            offset = if (newScale > 1.02f) {
                offset + panChange
            } else {
                Offset.Zero
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ObsidianBlack.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CharcoalElevated.copy(alpha = 0.85f))
                        .testTag("close_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close full screen photo",
                        tint = PureWhite
                    )
                }
            }

            // Bottom quick action bar if callbacks provided
            if (onKeep != null || onDelete != null || onSkip != null || onRestoreKeep != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CrimsonDelete.copy(alpha = 0.2f))
                                .border(1.dp, CrimsonDelete, CircleShape)
                                .clickable { onDelete() }
                                .testTag("fullscreen_delete_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Mark for deletion",
                                tint = CrimsonDelete
                            )
                        }
                    }
                    if (onSkip != null) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CharcoalElevated)
                                .border(1.dp, HairlineBorder, CircleShape)
                                .clickable { onSkip() }
                                .testTag("fullscreen_skip_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Skip photo",
                                tint = SilverText
                            )
                        }
                    }
                    if (onKeep != null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldKeep.copy(alpha = 0.2f))
                                .border(1.dp, EmeraldKeep, CircleShape)
                                .clickable { onKeep() }
                                .testTag("fullscreen_keep_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Keep photo",
                                tint = EmeraldKeep
                            )
                        }
                    }
                    if (onRestoreKeep != null) {
                        Button(
                            onClick = onRestoreKeep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("fullscreen_restore_keep_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldKeep,
                                contentColor = ObsidianBlack
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove from Queue & Keep", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
