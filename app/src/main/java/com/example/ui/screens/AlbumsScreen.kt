package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumSortOption
import com.example.data.model.MediaPermissionState
import com.example.ui.components.PermissionOnboardingCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHigh
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.viewmodel.GalleryUiState
import com.example.util.Formatters

@Composable
fun AlbumsScreen(
    galleryState: GalleryUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortSelected: (AlbumSortOption) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onSelectMorePhotos: () -> Unit,
    onStartAlbumCleaning: (AlbumItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val albums = galleryState.filteredAndSortedAlbums
    val allPhotos = galleryState.allPhotosSummary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite
                        )
                        Text(
                            text = "${galleryState.albums.size} folders • Newest photos first",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGrey
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CharcoalSurface)
                            .testTag("refresh_albums_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh gallery",
                            tint = PureWhite
                        )
                    }
                }
            }

            if (galleryState.permissionState != MediaPermissionState.FULL_ACCESS) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PermissionOnboardingCard(
                        permissionState = galleryState.permissionState,
                        onRequestFullOrSelected = onRequestPermission,
                        onManageSelected = onSelectMorePhotos
                    )
                }
            }

            if (galleryState.permissionState != MediaPermissionState.DENIED) {
                // Search & Sort Controls
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = galleryState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text("Search albums (Camera, Screenshots...)", color = MutedGrey)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MutedGrey
                                )
                            },
                            trailingIcon = {
                                if (galleryState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                            tint = SilverText
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CharcoalSurface,
                                unfocusedContainerColor = CharcoalSurface,
                                focusedBorderColor = PureWhite.copy(alpha = 0.6f),
                                unfocusedBorderColor = HairlineBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("album_search_input")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AlbumSortOption.entries) { option ->
                                val selected = galleryState.sortOption == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSortSelected(option) },
                                    label = { Text(option.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PureWhite,
                                        selectedLabelColor = ObsidianBlack,
                                        containerColor = CharcoalSurface,
                                        labelColor = SilverText
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = HairlineBorder,
                                        selectedBorderColor = PureWhite
                                    )
                                )
                            }
                        }
                    }
                }

                // Featured "All Photos" Card
                if (allPhotos != null && allPhotos.photoCount > 0 && galleryState.searchQuery.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AllPhotosFeaturedCard(
                            summary = allPhotos,
                            onStartCleaning = { onStartAlbumCleaning(allPhotos) }
                        )
                    }
                }

                // Loading / Empty / Album Grid
                if (galleryState.isLoading && albums.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp)
                        }
                    }
                } else if (albums.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = CharcoalSurface,
                            border = BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MutedGrey,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No matching albums found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Try clearing your search or refreshing the device gallery.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGrey
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = albums,
                        key = { it.bucketId }
                    ) { album ->
                        AlbumGridTile(
                            album = album,
                            onClick = { onStartAlbumCleaning(album) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllPhotosFeaturedCard(
    summary: AlbumItem,
    onStartCleaning: () -> Unit
) {
    val context = LocalContext.current
    val progress = if (summary.photoCount > 0) {
        (summary.reviewedCount.toFloat() / summary.photoCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onStartCleaning)
            .testTag("all_photos_album_card"),
        shape = MaterialTheme.shapes.large,
        color = CharcoalElevated,
        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalHigh),
                contentAlignment = Alignment.Center
            ) {
                if (summary.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(summary.coverUri)
                            .size(300, 300)
                            .crossfade(true)
                            .build(),
                        contentDescription = "All Photos thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesomeMotion,
                        contentDescription = null,
                        tint = PureWhite
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "All Photos",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
                Text(
                    text = "${Formatters.formatCount(summary.photoCount)} photos • ${Formatters.formatBytes(summary.totalSizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverText
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = EmeraldKeep,
                    trackColor = CharcoalHigh
                )
            }

            Button(
                onClick = onStartCleaning,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("start_all_photos_cleaning_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = ObsidianBlack
                ),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AlbumGridTile(
    album: AlbumItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val progress = if (album.photoCount > 0) {
        (album.reviewedCount.toFloat() / album.photoCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("album_grid_tile_${album.bucketId}"),
        shape = MaterialTheme.shapes.medium,
        color = CharcoalSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.18f)
                    .background(CharcoalElevated)
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(album.coverUri)
                            .size(450, 450)
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
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianBlack.copy(alpha = 0.78f)
                ) {
                    Text(
                        text = Formatters.formatCount(album.photoCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Formatters.formatBytes(album.totalSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                    Text(
                        text = "${album.reviewedCount}/${album.photoCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = EmeraldKeep,
                    trackColor = CharcoalHigh
                )
            }
        }
    }
}
