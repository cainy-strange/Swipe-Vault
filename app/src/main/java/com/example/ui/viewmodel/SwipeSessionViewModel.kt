package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PhotoItem
import com.example.data.model.SwipeDecisionType
import com.example.data.model.SwipeUndoAction
import com.example.data.repository.SwipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SwipeUiState(
    val bucketId: Long = -1L,
    val albumName: String = "All Photos",
    val isLoading: Boolean = true,
    val isSkippedMode: Boolean = false,
    val totalPhotosInScope: Int = 0,
    val keptCount: Int = 0,
    val deleteQueueCount: Int = 0,
    val skippedCount: Int = 0,
    val deck: List<PhotoItem> = emptyList(),
    val skippedPool: List<PhotoItem> = emptyList(),
    val canUndo: Boolean = false,
    val fullScreenPhoto: PhotoItem? = null,
    val errorMessage: String? = null
) {
    val currentPhoto: PhotoItem? get() = deck.firstOrNull()
    val nextPhoto: PhotoItem? get() = deck.getOrNull(1)
    val preloadUris: List<Uri> get() = deck.drop(1).take(3).map { it.contentUri }
    val reviewedCount: Int
        get() = (keptCount + deleteQueueCount + skippedCount).coerceAtMost(totalPhotosInScope)
    val progressFraction: Float
        get() = if (totalPhotosInScope > 0) {
            (reviewedCount.toFloat() / totalPhotosInScope.toFloat()).coerceIn(0f, 1f)
        } else 0f
    val progressPercent: Int
        get() = (progressFraction * 100f).roundToInt().coerceIn(0, 100)
}

class SwipeSessionViewModel(
    private val bucketId: Long,
    private val albumName: String,
    private val revisitSkippedOnly: Boolean,
    private val repository: SwipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SwipeUiState(
            bucketId = bucketId,
            albumName = albumName,
            isSkippedMode = revisitSkippedOnly
        )
    )
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    private var activeSessionId: Long = 0L
    private val undoStack = ArrayDeque<SwipeUndoAction>()

    init {
        loadDeck()
    }

    fun loadDeck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val prepared = repository.prepareSwipeDeck(bucketId)
                activeSessionId = repository.startOrResumeSession(
                    bucketId = bucketId,
                    albumName = albumName,
                    totalPhotosInScope = prepared.totalInScope
                )
                prepared
            }.onSuccess { prepared ->
                val initialDeck = if (revisitSkippedOnly) {
                    prepared.skippedPhotos
                } else {
                    prepared.unreviewedPhotos
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSkippedMode = revisitSkippedOnly,
                        totalPhotosInScope = prepared.totalInScope,
                        keptCount = prepared.keptCount,
                        deleteQueueCount = prepared.deleteQueueCount,
                        skippedCount = prepared.skippedCount,
                        deck = initialDeck,
                        skippedPool = if (revisitSkippedOnly) emptyList() else prepared.skippedPhotos,
                        canUndo = undoStack.isNotEmpty()
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Unable to load photos."
                    )
                }
            }
        }
    }

    fun swipeCurrentPhoto(decision: SwipeDecisionType) {
        val currentState = _uiState.value
        val photo = currentState.currentPhoto ?: return
        val remaining = currentState.deck.drop(1)

        var newKept = currentState.keptCount
        var newDelete = currentState.deleteQueueCount
        var newSkipped = currentState.skippedCount
        val newSkippedPool = currentState.skippedPool.toMutableList()

        val wasPreviouslySkipped = currentState.isSkippedMode ||
            newSkippedPool.any { it.mediaStoreId == photo.mediaStoreId }

        when (decision) {
            SwipeDecisionType.KEEP -> {
                newKept++
                if (wasPreviouslySkipped) {
                    newSkipped = (newSkipped - 1).coerceAtLeast(0)
                    newSkippedPool.removeAll { it.mediaStoreId == photo.mediaStoreId }
                }
            }
            SwipeDecisionType.DELETE_QUEUE -> {
                newDelete++
                if (wasPreviouslySkipped) {
                    newSkipped = (newSkipped - 1).coerceAtLeast(0)
                    newSkippedPool.removeAll { it.mediaStoreId == photo.mediaStoreId }
                }
            }
            SwipeDecisionType.SKIP -> {
                if (!wasPreviouslySkipped) {
                    newSkipped++
                }
                if (newSkippedPool.none { it.mediaStoreId == photo.mediaStoreId }) {
                    newSkippedPool.add(photo)
                }
            }
        }

        val previousDecision = if (currentState.isSkippedMode) SwipeDecisionType.SKIP else null
        undoStack.addLast(
            SwipeUndoAction(
                photo = photo,
                previousDecision = previousDecision,
                appliedDecision = decision
            )
        )

        _uiState.update {
            it.copy(
                deck = remaining,
                keptCount = newKept,
                deleteQueueCount = newDelete,
                skippedCount = newSkipped,
                skippedPool = newSkippedPool,
                canUndo = undoStack.isNotEmpty(),
                fullScreenPhoto = if (it.fullScreenPhoto?.mediaStoreId == photo.mediaStoreId) null else it.fullScreenPhoto
            )
        }

        viewModelScope.launch {
            repository.recordSwipeDecision(
                photo = photo,
                decision = decision,
                sessionId = activeSessionId,
                bucketIdScope = bucketId,
                totalInScope = currentState.totalPhotosInScope
            )
        }
    }

    fun undoLastSwipe() {
        val action = undoStack.removeLastOrNull() ?: return
        val currentState = _uiState.value

        var newKept = currentState.keptCount
        var newDelete = currentState.deleteQueueCount
        var newSkipped = currentState.skippedCount
        val newSkippedPool = currentState.skippedPool.toMutableList()

        when (action.appliedDecision) {
            SwipeDecisionType.KEEP -> newKept = (newKept - 1).coerceAtLeast(0)
            SwipeDecisionType.DELETE_QUEUE -> newDelete = (newDelete - 1).coerceAtLeast(0)
            SwipeDecisionType.SKIP -> {
                if (action.previousDecision != SwipeDecisionType.SKIP) {
                    newSkipped = (newSkipped - 1).coerceAtLeast(0)
                    newSkippedPool.removeAll { it.mediaStoreId == action.photo.mediaStoreId }
                }
            }
        }

        if (action.previousDecision == SwipeDecisionType.SKIP &&
            action.appliedDecision != SwipeDecisionType.SKIP
        ) {
            newSkipped++
            if (!currentState.isSkippedMode && newSkippedPool.none { it.mediaStoreId == action.photo.mediaStoreId }) {
                newSkippedPool.add(action.photo)
            }
        }

        val restoredDeck = listOf(action.photo) + currentState.deck.filterNot {
            it.mediaStoreId == action.photo.mediaStoreId
        }

        _uiState.update {
            it.copy(
                deck = restoredDeck,
                keptCount = newKept,
                deleteQueueCount = newDelete,
                skippedCount = newSkipped,
                skippedPool = newSkippedPool,
                canUndo = undoStack.isNotEmpty()
            )
        }

        viewModelScope.launch {
            repository.undoSwipeDecision(
                undoAction = action,
                sessionId = activeSessionId,
                bucketIdScope = bucketId,
                totalInScope = currentState.totalPhotosInScope
            )
        }
    }

    fun startRevisitingSkipped() {
        val skipped = _uiState.value.skippedPool
        if (skipped.isEmpty()) return
        _uiState.update {
            it.copy(
                isSkippedMode = true,
                deck = skipped,
                skippedPool = emptyList()
            )
        }
    }

    fun openFullScreen(photo: PhotoItem) {
        _uiState.update { it.copy(fullScreenPhoto = photo) }
    }

    fun closeFullScreen() {
        _uiState.update { it.copy(fullScreenPhoto = null) }
    }

    class Factory(
        private val bucketId: Long,
        private val albumName: String,
        private val revisitSkippedOnly: Boolean,
        private val repository: SwipeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwipeSessionViewModel(bucketId, albumName, revisitSkippedOnly, repository) as T
        }
    }
}
