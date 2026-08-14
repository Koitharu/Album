package org.koitharu.album.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

fun Modifier.snappedRotationGesture(
    isRotationEnabled: Boolean = true,
    rotationThresholdDegrees: Float = 7f,
    onRotationSaved: (Int) -> Unit = {},
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val animatedRotation = remember { Animatable(0f) }
    var savedRotation by remember { mutableIntStateOf(0) }
    var currentGestureRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(savedRotation, currentGestureRotation) {
        animatedRotation.snapTo(savedRotation + currentGestureRotation)
    }

    pointerInput(isRotationEnabled) {
        awaitPointerEventScope {
            while (true) {
                var event = awaitPointerEvent(PointerEventPass.Initial)
                var isGestureActive = event.changes.size > 1

                var isRotationStarted = false
                var accumulatedRotation = 0f

                while (isGestureActive) {
                    val rotationChange = if (isRotationEnabled) event.calculateRotation() else 0f
                    accumulatedRotation += rotationChange

                    if (isRotationStarted) {
                        currentGestureRotation += rotationChange
                    } else if (abs(accumulatedRotation) >= rotationThresholdDegrees) {
                        isRotationStarted = true
                        currentGestureRotation = accumulatedRotation
                    }

                    event = awaitPointerEvent(PointerEventPass.Initial)
                    isGestureActive = event.changes.size > 1
                }

                if (currentGestureRotation != 0f) {
                    val totalRotation = savedRotation + currentGestureRotation
                    val snapped = (totalRotation / 90f).roundToInt() * 90
                    val normalizedRotation = (snapped % 360 + 360) % 360

                    currentGestureRotation = 0f
                    savedRotation = normalizedRotation

                    scope.launch {
                        animatedRotation.animateTo(
                            targetValue = normalizedRotation.toFloat(),
                            animationSpec = tween(durationMillis = 300)
                        )
                        onRotationSaved(normalizedRotation)
                    }
                } else if (accumulatedRotation != 0f && !isRotationStarted) {
                    scope.launch {
                        animatedRotation.animateTo(
                            targetValue = savedRotation.toFloat(),
                            animationSpec = tween(durationMillis = 150)
                        )
                    }
                }
            }
        }
    }.graphicsLayer {
        rotationZ = animatedRotation.value
    }
}
