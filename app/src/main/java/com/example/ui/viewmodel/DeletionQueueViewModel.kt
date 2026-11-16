package com.example.ui.viewmodel

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SwipeDecisionEntity
import com.example.data.media.DeleteRequestOutcome
import com.example.data.repository.SwipeRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeletionQueueUiState(
    val queuedPhotos: List<SwipeDecisionEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isDeleting: Boolean = false,
    val previewPhoto: SwipeDecisionEntity? = null,
    val showConfirmDialog: Boolean = false,
    val statusBanner: String? = null
) {
    val selectedPhotos: List<SwipeDecisionEntity>
        get() = queuedPhotos.filter { it.mediaStoreId in selectedIds }

    val selectedBytes: Long
        get() = selectedPhotos.sumOf { it.sizeBytes }

    val totalQueueBytes: Long
        get() = queuedPhotos.sumOf { it.sizeBytes }

    val allSelected: Boolean
        get() = queuedPhotos.isNotEmpty() && selectedIds.size == queuedPhotos.size
}

sealed interface DeletionUiEvent {
    data class LaunchIntentSender(val intentSender: IntentSender) : DeletionUiEvent
    data class ShowMessage(val message: String) : DeletionUiEvent
}

class DeletionQueueViewModel(
    private val repository: SwipeRepository
) : ViewModel() {

    private val _deselectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isDeleting = MutableStateFlow(false)
    private val _previewPhoto = MutableStateFlow<SwipeDecisionEntity?>(null)
    private val _showConfirmDialog = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<DeletionUiEvent>()
    val events: SharedFlow<DeletionUiEvent> = _events.asSharedFlow()

    private var pendingDeleteItems: List<SwipeDecisionEntity> = emptyList()
    private var pendingStorageBeforeBytes: Long = 0L

    val uiState: StateFlow<DeletionQueueUiState> = combine(
        repository.observeDeletionQueue(),
        _deselectedIds,
        _isDeleting,
        _previewPhoto,
        _showConfirmDialog,
        _statusBanner
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val queue = values[0] as List<SwipeDecisionEntity>
        @Suppress("UNCHECKED_CAST")
        val deselected = values[1] as Set<Long>
        val isDeleting = values[2] as Boolean
        val preview = values[3] as SwipeDecisionEntity?
        val showConfirm = values[4] as Boolean
        val banner = values[5] as String?

        val selected = queue.map { it.mediaStoreId }.filter { it !in deselected }.toSet()
        DeletionQueueUiState(
            queuedPhotos = queue,
            selectedIds = selected,
            isDeleting = isDeleting,
            previewPhoto = preview,
            showConfirmDialog = showConfirm,
            statusBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeletionQueueUiState()
    )

    init {
        viewModelScope.launch {
            repository.pruneMissingQueueItems()
        }
    }

    fun toggleSelect(mediaStoreId: Long) {
        val current = _deselectedIds.value.toMutableSet()
        if (mediaStoreId in current) {
            current.remove(mediaStoreId)
        } else {
            current.add(mediaStoreId)
        }
        _deselectedIds.value = current
    }

    fun selectAll() {
        _deselectedIds.value = emptySet()
    }

    fun deselectAll() {
        _deselectedIds.value = uiState.value.queuedPhotos.map { it.mediaStoreId }.toSet()
    }

    fun removeFromQueue(mediaStoreId: Long) {
        viewModelScope.launch {
            repository.removeFromDeletionQueue(mediaStoreId)
            _deselectedIds.value = _deselectedIds.value - mediaStoreId
            if (_previewPhoto.value?.mediaStoreId == mediaStoreId) {
                _previewPhoto.value = null
            }
        }
    }

    fun keepSelected() {
        val selectedIds = uiState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.removeMultipleFromDeletionQueue(selectedIds)
            _deselectedIds.value = _deselectedIds.value - selectedIds.toSet()
            _statusBanner.value = "Restored ${selectedIds.size} photo(s) to Kept"
        }
    }

    fun openPreview(item: SwipeDecisionEntity) {
        _previewPhoto.value = item
    }

    fun closePreview() {
        _previewPhoto.value = null
    }

    fun openConfirmDialog() {
        if (uiState.value.selectedPhotos.isNotEmpty()) {
            _showConfirmDialog.value = true
        }
    }

    fun dismissConfirmDialog() {
        _showConfirmDialog.value = false
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun confirmAndStartOsDelete() {
        val targets = uiState.value.selectedPhotos
        if (targets.isEmpty()) {
            _showConfirmDialog.value = false
            return
        }
        _showConfirmDialog.value = false
        viewModelScope.launch {
            _isDeleting.value = true
            pendingDeleteItems = targets
            val (storageBefore, outcome) = repository.requestOsDeletion(targets)
            pendingStorageBeforeBytes = storageBefore

            when (outcome) {
                is DeleteRequestOutcome.RequiresConfirmation -> {
                    _events.emit(DeletionUiEvent.LaunchIntentSender(outcome.intentSender))
                }
                is DeleteRequestOutcome.CompletedDirectly -> {
                    val result = repository.finalizeConfirmedDeletion(
                        requestedItems = targets,
                        storageBeforeBytes = storageBefore
                    )
                    _isDeleting.value = false
                    pendingDeleteItems = emptyList()
                    _statusBanner.value = buildSuccessMessage(
                        result.confirmedDeletedCount,
                        result.confirmedFreedBytes,
                        result.updatedStorage.availableBytes
                    )
                }
                is DeleteRequestOutcome.Failed -> {
                    _isDeleting.value = false
                    pendingDeleteItems = emptyList()
                    _events.emit(DeletionUiEvent.ShowMessage(outcome.message))
                }
            }
        }
    }

    fun onOsDeleteResult(approved: Boolean) {
        val batch = pendingDeleteItems
        val storageBefore = pendingStorageBeforeBytes
        pendingDeleteItems = emptyList()

        if (!approved) {
            _isDeleting.value = false
            _statusBanner.value = "Deletion cancelled — all ${batch.size} photo(s) remain untouched"
            return
        }

        viewModelScope.launch {
            val result = repository.finalizeConfirmedDeletion(
                requestedItems = batch,
                storageBeforeBytes = storageBefore
            )
            _isDeleting.value = false
            _statusBanner.value = if (result.confirmedDeletedCount > 0) {
                buildSuccessMessage(
                    result.confirmedDeletedCount,
                    result.confirmedFreedBytes,
                    result.updatedStorage.availableBytes
                )
            } else {
                "No photos were removed — device gallery unchanged"
            }
        }
    }

    private fun buildSuccessMessage(
        deletedCount: Int,
        freedBytes: Long,
        recheckedAvailableBytes: Long
    ): String {
        val freed = Formatters.formatBytes(freedBytes)
        val freeNow = Formatters.formatBytes(recheckedAvailableBytes)
        return "Confirmed deleted $deletedCount photo(s) • $freed freed (Device free: $freeNow)"
    }

    class Factory(
        private val repository: SwipeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeletionQueueViewModel(repository) as T
        }
    }
}
