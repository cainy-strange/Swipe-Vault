package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.CleaningSessionEntity
import com.example.data.model.AlbumItem
import com.example.data.model.CleaningStats
import com.example.data.model.MediaPermissionState
import com.example.ui.components.PermissionOnboardingCard
import com.example.ui.components.StorageMeterBar
import com.example.ui.components.SwipeVaultLogo
import com.example.ui.theme.AmberSkipAccent
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHigh
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.CrimsonDeleteContainer
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.viewmodel.GalleryUiState
import com.example.util.Formatters

@Composable
fun HomeScreen(
    galleryState: GalleryUiState,
    stats: CleaningStats,
    recentSessions: List<CleaningSessionEntity>,
    onRequestGalleryPermission: () -> Unit,
    onSelectMorePhotos: () -> Unit,
    onStartAllPhotosCleaning: () -> Unit,
    onSelectAlbum: (AlbumItem) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenDeletionQueue: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwipeVaultLogo()
                    PermissionStatusChip(state = galleryState.permissionState)
                }
            }

            // Hero Welcome Banner
            item {
                HomeHeroBanner(
                    galleryState = galleryState,
                    onRequestPermission = onRequestGalleryPermission,
                    onStartCleaning = onStartAllPhotosCleaning
                )
            }

            // Permission onboarding or Android 14+ partial access banner
            if (galleryState.permissionState != MediaPermissionState.FULL_ACCESS) {
                item {
                    PermissionOnboardingCard(
                        permissionState = galleryState.permissionState,
                        onRequestFullOrSelected = onRequestGalleryPermission,
                        onManageSelected = onSelectMorePhotos
                    )
                }
            }

            // Pending Deletion Queue Callout
            if (stats.totalQueuedForDeletion > 0) {
                item {
                    PendingQueueBanner(
                        queuedCount = stats.totalQueuedForDeletion,
                        pendingBytes = stats.pendingDeleteBytes,
                        onClick = onOpenDeletionQueue
                    )
                }
            }

            // Quick Album Selection Strip
            if (galleryState.permissionState != MediaPermissionState.DENIED) {
                item {
                    SectionHeader(
                        title = "Albums",
                        actionLabel = "See All",
                        onAction = onOpenAlbums
                    )
                }

                item {
                    if (galleryState.isLoading && galleryState.albums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
                        }
                    } else if (galleryState.albums.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = CharcoalSurface,
                            border = BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MutedGrey
                                )
                                Text(
                                    text = "No accessible photos found on this device yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SilverText
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = galleryState.albums.take(10),
                                key = { it.bucketId }
                            ) { album ->
                                QuickAlbumCard(
                                    album = album,
                                    onClick = { onSelectAlbum(album) }
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Cleaning Statistics Grid
            item {
                SectionHeader(
                    title = "Vault Overview",
                    actionLabel = "Analytics",
                    onAction = onOpenStats
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniStatTile(
                            label = "REVIEWED",
                            value = Formatters.formatCount(stats.totalReviewed),
                            accent = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatTile(
                            label = "KEPT",
                            value = Formatters.formatCount(stats.totalKept),
                            accent = EmeraldKeep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniStatTile(
                            label = "DELETED",
                            value = Formatters.formatCount(stats.totalDeleted),
                            accent = CrimsonDelete,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStatTile(
                            label = "CONFIRMED FREED",
                            value = Formatters.formatBytes(stats.actualRecoveredBytes),
                            accent = EmeraldKeep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Cleaning Sessions
            if (recentSessions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recent Sessions",
                        actionLabel = null,
                        onAction = {}
                    )
                }
                items(
                    items = recentSessions.take(4),
                    key = { it.id }
                ) { session ->
                    RecentSessionCard(
                        session = session,
                        onClick = {
                            onSelectAlbum(
                                AlbumItem(
                                    bucketId = session.bucketId,
                                    name = session.albumName,
                                    coverUri = null,
                                    photoCount = session.totalPhotosInScope,
                                    totalSizeBytes = 0L,
                                    latestTimestamp = session.updatedAtMillis,
                                    reviewedCount = session.reviewedCount
                                )
                            )
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusChip(state: MediaPermissionState) {
    val (label, dotColor) = when (state) {
        MediaPermissionState.FULL_ACCESS -> "FULL LIBRARY" to EmeraldKeep
        MediaPermissionState.PARTIAL_ACCESS -> "SELECTED ONLY" to AmberSkipAccent
        MediaPermissionState.DENIED -> "LOCKED" to CrimsonDelete
    }
    Surface(
        shape = CircleShape,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SilverText
            )
        }
    }
}

@Composable
private fun HomeHeroBanner(
    galleryState: GalleryUiState,
    onRequestPermission: () -> Unit,
    onStartCleaning: () -> Unit
) {
    val allSummary = galleryState.allPhotosSummary
    val hasAccess = galleryState.permissionState != MediaPermissionState.DENIED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_vault_hero),
                contentDescription = "SwipeVault minimal gallery banner",
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier
                    .matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianBlack.copy(alpha = 0.35f),
                                ObsidianBlack.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldKeep,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LOCAL GALLERY VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                }

                Text(
                    text = if (allSummary != null && allSummary.photoCount > 0) {
                        "${Formatters.formatCount(allSummary.photoCount)} photos ready to review"
                    } else {
                        "Declutter your camera roll in seconds"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = PureWhite
                )

                Text(
                    text = if (allSummary != null && allSummary.photoCount > 0) {
                        "${Formatters.formatBytes(allSummary.totalSizeBytes)} across ${galleryState.albums.size} albums • Swipe right to keep, left to queue"
                    } else {
                        "Swipe right to keep, left to queue safely, up to skip. Nothing deletes without your OS confirmation."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverText
                )

                StorageMeterBar(storage = galleryState.storageSnapshot)

                Button(
                    onClick = if (hasAccess) onStartCleaning else onRequestPermission,
                    enabled = !hasAccess || (allSummary?.photoCount ?: 0) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("home_primary_clean_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = CharcoalHigh,
                        disabledContentColor = MutedGrey
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasAccess) "Clean All Photos (Newest First)" else "Grant Gallery Access",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingQueueBanner(
    queuedCount: Int,
    pendingBytes: Long,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("home_pending_queue_banner"),
        shape = MaterialTheme.shapes.medium,
        color = CrimsonDeleteContainer,
        border = BorderStroke(1.dp, CrimsonDelete.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CrimsonDelete.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Deletion queue",
                    tint = CrimsonDelete
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$queuedCount photo(s) queued for deletion",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Text(
                    text = "${Formatters.formatBytes(pendingBytes)} est. pending • Tap to review & confirm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverText
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Open deletion queue",
                tint = PureWhite
            )
        }
    }
}

@Composable
private fun QuickAlbumCard(
    album: AlbumItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .width(146.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("home_album_card_${album.bucketId}"),
        shape = MaterialTheme.shapes.medium,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(CharcoalElevated)
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(album.coverUri)
                            .size(400, 400)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = MutedGrey,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianBlack.copy(alpha = 0.78f)
                ) {
                    Text(
                        text = "${album.photoCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatBytes(album.totalSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey
                )
            }
        }
    }
}

@Composable
private fun MiniStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedGrey
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    session: CleaningSessionEntity,
    onClick: () -> Unit
) {
    val progress = if (session.totalPhotosInScope > 0) {
        (session.reviewedCount.toFloat() / session.totalPhotosInScope.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("recent_session_${session.id}"),
        shape = MaterialTheme.shapes.medium,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = SilverText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = session.albumName,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite
                    )
                }
                Text(
                    text = Formatters.formatDateTime(session.updatedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = if (session.isCompleted) EmeraldKeep else PureWhite,
                trackColor = CharcoalHigh
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${session.reviewedCount} / ${session.totalPhotosInScope} reviewed",
                    style = MaterialTheme.typography.labelSmall,
                    color = SilverText
                )
                Text(
                    text = "Kept ${session.keptCount} • Queued ${session.deletedQueueCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, color = SilverText)
            }
        }
    }
}
