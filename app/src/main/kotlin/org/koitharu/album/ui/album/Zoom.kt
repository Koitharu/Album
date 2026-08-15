package org.koitharu.album.ui.album

import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.handleZoomGesture(block: (Float) -> Unit) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1) {
                val zoomChange = event.calculateZoom()
                block(zoomChange)
                event.changes.forEach { it.consume() }
            }
        }
    }
}