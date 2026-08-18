package org.koitharu.album.util

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

fun Modifier.snappedRotationGesture(
    key: Any? = null,
    isRotationEnabled: Boolean = true,
    rotationThresholdDegrees: Float = 7f,
    onRotated: (Int) -> Unit = {},
): Modifier = composed {
    val currentRotation = remember(key) { Animatable(0f) }
    var isGestureInProgress by remember(key) { mutableStateOf(false) }
    var targetRotation by remember(key) { mutableFloatStateOf(0f) }
    LaunchedEffect(targetRotation, isGestureInProgress) {
        if (!isGestureInProgress) {
            currentRotation.animateTo(targetRotation)
            val intValue = (targetRotation.roundToInt() % 360 + 360) % 360
            if (intValue != 0) {
                onRotated(intValue)
            }
        } else {
            currentRotation.snapTo(targetRotation)
        }
    }

    pointerInput(isRotationEnabled) {
        if (isRotationEnabled) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                var gestureRotation = 0f
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val rotation = event.calculateRotation()
                    gestureRotation += rotation
                    if (isGestureInProgress || gestureRotation.absoluteValue >= rotationThresholdDegrees) {
                        targetRotation = gestureRotation
                        isGestureInProgress = true
                        event.consume()
                    }
                } while (event.changes.any { it.pressed })

                val aligned = (targetRotation / 90f).roundToInt() * 90
                targetRotation = aligned.toFloat()
                isGestureInProgress = false
            }
        }
    }.graphicsLayer(rotationZ = currentRotation.value)
}

private fun PointerEvent.consume() = changes.forEach { it.consume() }