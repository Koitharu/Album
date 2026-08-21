package org.koitharu.album.util

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.launch

fun Modifier.slideUpToClose(
    onClose: () -> Unit,
) = composed {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    graphicsLayer {
        val maxDistance = size.height
        val progress = if (maxDistance > 0) (-offsetY.value / maxDistance) else 0f
        translationY = offsetY.value
        alpha = (1f - progress).coerceIn(0f, 1f)
    }.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
            },
            onDragCancel = {
                velocityTracker.resetTracking()
            },
            onDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)
                change.consume()
                scope.launch {
                    val newOffset =
                        (offsetY.value + dragAmount.y).coerceIn(-size.height.toFloat(), 0f)
                    offsetY.snapTo(newOffset)
                }
            },
            onDragEnd = {
                val velocity = velocityTracker.calculateVelocity()
                if (velocity.y <= -viewConfiguration.minimumFlingVelocity) {
                    onClose()
                } else {
                    val threshold = size.height / 4f
                    if (offsetY.value <= -threshold) {
                        onClose()
                    } else {
                        scope.launch {
                            offsetY.animateTo(0f)
                        }
                    }
                }
            }
        )
    }
}