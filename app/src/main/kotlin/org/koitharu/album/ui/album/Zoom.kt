package org.koitharu.album.ui.album

import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.handleZoomGesture(block: (Float) -> Unit) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            // 1. Snatch pointer events before children see them
            val event = awaitPointerEvent(PointerEventPass.Initial)

            // 2. ONLY intervene if 2 or more fingers are tracking (pinching)
            if (event.changes.size > 1) {
                // Calculate the math directly using Compose's native extension
                val zoomChange = event.calculateZoom()
                block(zoomChange)
                // 3. Consume the pointers so children don't scroll or register clicks
                event.changes.forEach { it.consume() }
            }
        }
    }
}