package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object AlbumsRoute

@Serializable
data class SwipeRoute(
    val bucketId: Long,
    val albumName: String,
    val revisitSkippedOnly: Boolean = false
)

@Serializable
data object DeletionQueueRoute

@Serializable
data object StatsRoute
