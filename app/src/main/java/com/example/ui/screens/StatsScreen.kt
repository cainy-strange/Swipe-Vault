package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.CleaningSessionEntity
import com.example.data.local.DeletionHistoryEntity
import com.example.data.model.CleaningStats
import com.example.data.model.StorageSnapshot
import com.example.ui.components.StorageMeterBar
import com.example.ui.theme.AmberSkipAccent
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalHigh
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldKeepContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.util.Formatters

@Composable
fun StatsScreen(
    stats: CleaningStats,
    storageSnapshot: StorageSnapshot,
    sessions: List<CleaningSessionEntity>,
    deletionHistory: List<DeletionHistoryEntity>,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.displaySmall,
                        color = PureWhite
                    )
                    Text(
                        text = "Verified storage telemetry & cleaning history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGrey
                    )
                }
            }

            // Confirmed Recovered Storage Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = CharcoalElevated,
                    border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = EmeraldKeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CONFIRMED STORAGE RECOVERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldKeep
                            )
                        }

                        Text(
                            text = Formatters.formatBytes(stats.actualRecoveredBytes),
                            style = MaterialTheme.typography.displayLarge,
                            color = PureWhite
                        )

                        Text(
                            text = "Reported strictly after OS deletion confirmation and device storage recheck.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverText
                        )

                        if (stats.totalQueuedForDeletion > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = CharcoalSurface,
                                border = BorderStroke(1.dp, HairlineBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pending in queue (${stats.totalQueuedForDeletion} photos)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberSkipAccent
                                    )
                                    Text(
                                        text = "~${Formatters.formatBytes(stats.pendingDeleteBytes)} est.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberSkipAccent
                                    )
                                }
                            }
                        }

                        StorageMeterBar(storage = storageSnapshot)
                    }
                }
            }

            // 2x2 Telemetry Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryCard(
                            title = "PHOTOS REVIEWED",
                            value = Formatters.formatCount(stats.totalReviewed),
                            accent = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "PHOTOS RETAINED",
                            value = Formatters.formatCount(stats.totalKept),
                            accent = EmeraldKeep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryCard(
                            title = "PHOTOS DELETED",
                            value = Formatters.formatCount(stats.totalDeleted),
                            accent = CrimsonDelete,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "SESSIONS COMPLETED",
                            value = Formatters.formatCount(stats.sessionsCompleted),
                            accent = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Keep vs Delete Ratio Bar
            if (stats.totalKept + stats.totalDeleted > 0) {
                item {
                    val total = (stats.totalKept + stats.totalDeleted).toFloat()
                    val keptRatio = (stats.totalKept / total).coerceIn(0f, 1f)
                    val deletedRatio = (1f - keptRatio).coerceIn(0f, 1f)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = CharcoalSurface,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "DECISION BREAKDOWN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedGrey
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalHigh)
                            ) {
                                if (keptRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(keptRatio.coerceAtLeast(0.01f))
                                            .height(10.dp)
                                            .background(EmeraldKeep)
                                    )
                                }
                                if (deletedRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(deletedRatio.coerceAtLeast(0.01f))
                                            .height(10.dp)
                                            .background(CrimsonDelete)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Retained ${(keptRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldKeep
                                )
                                Text(
                                    text = "Deleted ${(deletedRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonDelete
                                )
                            }
                        }
                    }
                }
            }

            // Confirmed Deletion Batches
            if (deletionHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Confirmed Deletions",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite
                    )
                }
                val batches = deletionHistory.groupBy { it.batchId }.entries.toList()
                items(
                    items = batches.take(10),
                    key = { it.key }
                ) { (batchId, entries) ->
                    val freed = entries.sumOf { it.sizeBytes }
                    val storageAfter = entries.firstOrNull()?.storageAfterBytes ?: 0L
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = CharcoalSurface,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldKeepContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = EmeraldKeep,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entries.size} photo(s) deleted • ${Formatters.formatBytes(freed)} freed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = "${Formatters.formatDateTime(batchId)} • Rechecked free: ${Formatters.formatBytes(storageAfter)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGrey
                                )
                            }
                        }
                    }
                }
            }

            // Cleaning Sessions History
            item {
                Text(
                    text = "Cleaning History",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = CharcoalSurface,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                tint = MutedGrey,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No cleaning sessions yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Text(
                                text = "Select an album and swipe through photos to track your progress here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGrey
                            )
                        }
                    }
                }
            } else {
                items(
                    items = sessions.take(15),
                    key = { it.id }
                ) { session ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = CharcoalSurface,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = session.albumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Reviewed ${session.reviewedCount}/${session.totalPhotosInScope} • Kept ${session.keptCount} • Deleted ${session.confirmedDeletedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverText
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (session.isCompleted) EmeraldKeepContainer else CharcoalHigh
                            ) {
                                Text(
                                    text = if (session.isCompleted) "DONE" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (session.isCompleted) EmeraldKeep else SilverText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
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
                text = title,
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
