package org.koitharu.album.util

import androidx.compose.ui.geometry.Rect

operator fun Rect.times(factor: Float): Rect {
    val c = center
    val hw = width / 2f * factor
    val hh = height / 2f * factor
    return Rect(c.x - hw, c.y - hh, c.x + hw, c.y + hh)
}