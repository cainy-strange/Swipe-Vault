package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PhotoItem
import com.example.data.model.SwipeDecisionType
import com.example.ui.theme.AmberSkipAccent
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
class SwipeDeckController(
    private val scope: CoroutineScope,
    private val onDecisionCommitted: (SwipeDecisionType) -> Unit
) {
    internal val offset = Animatable(Offset.Zero, Offset.VectorConverter)
    internal var isAnimatingOut by mutableStateOf(false)

    fun programSwipe(decision: SwipeDecisionType, containerWidthPx: Float, containerHeightPx: Float) {
        if (isAnimatingOut) return
        isAnimatingOut = true
        scope.launch {
            val target = when (decision) {
                SwipeDecisionType.KEEP -> Offset(containerWidthPx * 1.45f, 40f)
                SwipeDecisionType.DELETE_QUEUE -> Offset(-containerWidthPx * 1.45f, 40f)
                SwipeDecisionType.SKIP -> Offset(0f, -containerHeightPx * 1.35f)
            }
            offset.animateTo(target, animationSpec = tween(durationMillis = 240))
            onDecisionCommitted(decision)
            offset.snapTo(Offset.Zero)
            isAnimatingOut = false
        }
    }
}

@Composable
fun rememberSwipeDeckController(
    onDecisionCommitted: (SwipeDecisionType) -> Unit
): SwipeDeckController {
    val scope = rememberCoroutineScope()
    return remember(onDecisionCommitted) {
        SwipeDeckController(scope, onDecisionCommitted)
    }
}

@Composable
fun TinderSwipeDeck(
    currentPhoto: PhotoItem,
    nextPhoto: PhotoItem?,
    controller: SwipeDeckController,
    onSwipeCommitted: (SwipeDecisionType) -> Unit,
    onTapPhoto: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var pinchZoom by remember(currentPhoto.mediaStoreId) { mutableFloatStateOf(1f) }
    var pinchPan by remember(currentPhoto.mediaStoreId) { mutableStateOf(Offset.Zero) }
    var crossedThreshold by remember(currentPhoto.mediaStoreId) { mutableStateOf<SwipeDecisionType?>(null) }

    LaunchedEffect(currentPhoto.mediaStoreId) {
        controller.offset.snapTo(Offset.Zero)
        controller.isAnimatingOut = false
        pinchZoom = 1f
        pinchPan = Offset.Zero
        crossedThreshold = null
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val swipeThresholdX = (widthPx * 0.28f).coerceAtLeast(140f)
        val swipeThresholdY = (heightPx * 0.22f).coerceAtLeast(130f)

        val dragOffset = controller.offset.value
        val dragMagnitude = maxOf(
            abs(dragOffset.x) / swipeThresholdX,
            (-dragOffset.y) / swipeThresholdY
        ).coerceIn(0f, 1f)

        // Background next card preview with subtle parallax scale
        if (nextPhoto != null) {
            val nextScale = 0.93f + (0.07f * dragMagnitude)
            val nextAlpha = 0.6f + (0.4f * dragMagnitude)
            SwipePhotoCardContent(
                photo = nextPhoto,
                overlayDecision = null,
                overlayProgress = 0f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = nextScale
                        scaleY = nextScale
                        alpha = nextAlpha
                    }
            )
        }

        // Active direction & intensity
        val activeDirection: SwipeDecisionType? = when {
            dragOffset.x > 24f && abs(dragOffset.x) >= abs(dragOffset.y) * 0.8f -> SwipeDecisionType.KEEP
            dragOffset.x < -24f && abs(dragOffset.x) >= abs(dragOffset.y) * 0.8f -> SwipeDecisionType.DELETE_QUEUE
            dragOffset.y < -28f -> SwipeDecisionType.SKIP
            else -> null
        }

        val overlayProgress = when (activeDirection) {
            SwipeDecisionType.KEEP, SwipeDecisionType.DELETE_QUEUE ->
                (abs(dragOffset.x) / swipeThresholdX).coerceIn(0f, 1f)
            SwipeDecisionType.SKIP ->
                ((-dragOffset.y) / swipeThresholdY).coerceIn(0f, 1f)
            null -> 0f
        }

        val thresholdDirection: SwipeDecisionType? = when {
            dragOffset.x >= swipeThresholdX -> SwipeDecisionType.KEEP
            dragOffset.x <= -swipeThresholdX -> SwipeDecisionType.DELETE_QUEUE
            dragOffset.y <= -swipeThresholdY && abs(dragOffset.x) < swipeThresholdX * 0.85f -> SwipeDecisionType.SKIP
            else -> null
        }

        LaunchedEffect(thresholdDirection) {
            if (thresholdDirection != null && thresholdDirection != crossedThreshold) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            crossedThreshold = thresholdDirection
        }

        val rotationDegrees = (dragOffset.x / widthPx) * 14f

        SwipePhotoCardContent(
            photo = currentPhoto,
            overlayDecision = activeDirection,
            overlayProgress = overlayProgress,
            modifier = Modifier
                .fillMaxSize()
                .testTag("active_swipe_card")
                .graphicsLayer {
                    translationX = dragOffset.x + pinchPan.x
                    translationY = dragOffset.y + pinchPan.y
                    rotationZ = rotationDegrees
                    scaleX = pinchZoom
                    scaleY = pinchZoom
                }
                .pointerInput(currentPhoto.mediaStoreId) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDrag = Offset.Zero
                        var movedPastSlop = false
                        var wasMultiTouch = false

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.count { it.pressed }
                            if (activePointers >= 2) {
                                wasMultiTouch = true
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                if (zoomChange != 1f || centroidSize > 0f) {
                                    val newZoom = (pinchZoom * zoomChange).coerceIn(1f, 3.5f)
                                    pinchZoom = newZoom
                                    pinchPan = if (newZoom > 1.02f) pinchPan + panChange else Offset.Zero
                                    event.changes.forEach { it.consume() }
                                }
                            } else if (!wasMultiTouch && !controller.isAnimatingOut) {
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    val delta = change.positionChange()
                                    totalDrag += delta
                                    if (!movedPastSlop && totalDrag.getDistance() > touchSlop) {
                                        movedPastSlop = true
                                    }
                                    if (movedPastSlop) {
                                        change.consume()
                                        scope.launch {
                                            controller.offset.snapTo(controller.offset.value + delta)
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (wasMultiTouch) {
                            pinchZoom = 1f
                            pinchPan = Offset.Zero
                        } else if (!movedPastSlop && !controller.isAnimatingOut) {
                            onTapPhoto(currentPhoto)
                        } else if (!controller.isAnimatingOut) {
                            val currentOffset = controller.offset.value
                            val commitDecision = when {
                                currentOffset.x >= swipeThresholdX -> SwipeDecisionType.KEEP
                                currentOffset.x <= -swipeThresholdX -> SwipeDecisionType.DELETE_QUEUE
                                currentOffset.y <= -swipeThresholdY &&
                                    abs(currentOffset.x) < swipeThresholdX * 0.85f -> SwipeDecisionType.SKIP
                                else -> null
                            }

                            if (commitDecision != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                controller.isAnimatingOut = true
                                scope.launch {
                                    val exitTarget = when (commitDecision) {
                                        SwipeDecisionType.KEEP -> Offset(widthPx * 1.45f, currentOffset.y + 80f)
                                        SwipeDecisionType.DELETE_QUEUE -> Offset(-widthPx * 1.45f, currentOffset.y + 80f)
                                        SwipeDecisionType.SKIP -> Offset(currentOffset.x * 0.4f, -heightPx * 1.35f)
                                    }
                                    controller.offset.animateTo(
                                        exitTarget,
                                        animationSpec = tween(durationMillis = 220)
                                    )
                                    onSwipeCommitted(commitDecision)
                                    controller.offset.snapTo(Offset.Zero)
                                    controller.isAnimatingOut = false
                                }
                            } else {
                                scope.launch {
                                    controller.offset.animateTo(
                                        Offset.Zero,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun SwipePhotoCardContent(
    photo: PhotoItem,
    overlayDecision: SwipeDecisionType?,
    overlayProgress: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(28.dp)

    val borderColor = when (overlayDecision) {
        SwipeDecisionType.KEEP -> EmeraldKeep.copy(alpha = 0.25f + 0.75f * overlayProgress)
        SwipeDecisionType.DELETE_QUEUE -> CrimsonDelete.copy(alpha = 0.25f + 0.75f * overlayProgress)
        SwipeDecisionType.SKIP -> AmberSkipAccent.copy(alpha = 0.25f + 0.65f * overlayProgress)
        null -> HairlineBorder
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.5.dp, borderColor), shape),
        shape = shape,
        color = CharcoalSurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.contentUri)
                    .size(1200, 1600)
                    .crossfade(160)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom editorial gradient for legible metadata
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                ObsidianBlack.copy(alpha = 0.55f),
                                ObsidianBlack.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = photo.bucketName,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldKeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CharcoalElevated.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = Formatters.formatBytes(photo.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = Formatters.formatDate(photo.dateTakenMillis),
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite
                    )
                    Text(
                        text = buildString {
                            append(photo.displayName)
                            if (photo.width > 0 && photo.height > 0) {
                                append(" • ${photo.width}×${photo.height}")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Directional swipe color veil + stamp badge
            if (overlayDecision != null && overlayProgress > 0.05f) {
                val veilColor = when (overlayDecision) {
                    SwipeDecisionType.KEEP -> EmeraldKeep.copy(alpha = overlayProgress * 0.24f)
                    SwipeDecisionType.DELETE_QUEUE -> CrimsonDelete.copy(alpha = overlayProgress * 0.26f)
                    SwipeDecisionType.SKIP -> Color(0xFF9CA3AF).copy(alpha = overlayProgress * 0.22f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(veilColor)
                )

                val stampAlpha = (overlayProgress * 1.4f).coerceIn(0f, 1f)
                when (overlayDecision) {
                    SwipeDecisionType.KEEP -> {
                        SwipeStampBadge(
                            label = "KEEP",
                            color = EmeraldKeep,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(24.dp)
                                .rotate(-12f)
                                .graphicsLayer { alpha = stampAlpha }
                        )
                    }
                    SwipeDecisionType.DELETE_QUEUE -> {
                        SwipeStampBadge(
                            label = "DELETE",
                            color = CrimsonDelete,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .rotate(12f)
                                .graphicsLayer { alpha = stampAlpha }
                        )
                    }
                    SwipeDecisionType.SKIP -> {
                        SwipeStampBadge(
                            label = "SKIP",
                            color = PureWhite,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp)
                                .graphicsLayer { alpha = stampAlpha }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeStampBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianBlack.copy(alpha = 0.72f),
        border = BorderStroke(2.5.dp, color)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}
