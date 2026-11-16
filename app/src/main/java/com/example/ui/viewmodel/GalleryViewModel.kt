package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CleaningSessionEntity
import com.example.data.local.DeletionHistoryEntity
import com.example.data.media.PermissionHelper
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumSortOption
import com.example.data.model.CleaningStats
import com.example.data.model.MediaPermissionState
import com.example.data.model.StorageSnapshot
import com.example.data.repository.SwipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryUiState(
    val permissionState: MediaPermissionState = MediaPermissionState.DENIED,
    val isLoading: Boolean = false,
    val albums: List<AlbumItem> = emptyList(),
    val allPhotosSummary: AlbumItem? = null,
    val sortOption: AlbumSortOption = AlbumSortOption.PHOTO_COUNT,
    val searchQuery: String = "",
    val storageSnapshot: StorageSnapshot = StorageSnapshot(0L, 0L),
    val errorMessage: String? = null
) {
    val filteredAndSortedAlbums: List<AlbumItem>
        get() {
            val filtered = if (searchQuery.isBlank()) {
                albums
            } else {
                albums.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            }
            return when (sortOption) {
                AlbumSortOption.PHOTO_COUNT -> filtered.sortedByDescending { it.photoCount }
                AlbumSortOption.STORAGE_SIZE -> filtered.sortedByDescending { it.totalSizeBytes }
                AlbumSortOption.RECENT -> filtered.sortedByDescending { it.latestTimestamp }
                AlbumSortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            }
        }
}

class GalleryViewModel(
    private val appContext: Context,
    private val repository: SwipeRepository
) : ViewModel() {

    private val _permissionState = MutableStateFlow(PermissionHelper.getMediaPermissionState(appContext))
    private val _isLoading = MutableStateFlow(false)
    private val _albums = MutableStateFlow<List<AlbumItem>>(emptyList())
    private val _allPhotosSummary = MutableStateFlow<AlbumItem?>(null)
    private val _sortOption = MutableStateFlow(AlbumSortOption.PHOTO_COUNT)
    private val _searchQuery = MutableStateFlow("")
    private val _storageSnapshot = MutableStateFlow(repository.getStorageSnapshot())
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GalleryUiState> = combine(
        _permissionState,
        _isLoading,
        _albums,
        _allPhotosSummary,
        _sortOption,
        _searchQuery,
        _storageSnapshot,
        _errorMessage
    ) { values ->
        GalleryUiState(
            permissionState = values[0] as MediaPermissionState,
            isLoading = values[1] as Boolean,
            albums = values[2] as List<AlbumItem>,
            allPhotosSummary = values[3] as AlbumItem?,
            sortOption = values[4] as AlbumSortOption,
            searchQuery = values[5] as String,
            storageSnapshot = values[6] as StorageSnapshot,
            errorMessage = values[7] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState(
            permissionState = _permissionState.value,
            storageSnapshot = _storageSnapshot.value
        )
    )

    val cleaningStats: StateFlow<CleaningStats> = repository.observeCleaningStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CleaningStats())

    val recentSessions: StateFlow<List<CleaningSessionEntity>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletionHistory: StateFlow<List<DeletionHistoryEntity>> = repository.observeDeletionHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedDeletionCount: StateFlow<Int> = repository.observeDeletionQueue()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refreshGallery()
    }

    fun refreshGallery() {
        val currentPermission = PermissionHelper.getMediaPermissionState(appContext)
        _permissionState.value = currentPermission
        _storageSnapshot.value = repository.getStorageSnapshot()

        if (currentPermission == MediaPermissionState.DENIED) {
            _albums.value = emptyList()
            _allPhotosSummary.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching {
                repository.pruneMissingQueueItems()
                repository.loadGalleryOverview()
            }.onSuccess { overview ->
                _albums.value = overview.albums
                _allPhotosSummary.value = overview.allPhotosSummary
                _storageSnapshot.value = overview.storage
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to load device gallery."
            }
            _isLoading.value = false
        }
    }

    fun onSortOptionSelected(option: AlbumSortOption) {
        _sortOption.value = option
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    class Factory(
        private val appContext: Context,
        private val repository: SwipeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(appContext, repository) as T
        }
    }
}
