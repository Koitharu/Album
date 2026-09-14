package org.koitharu.album.util

import androidx.compose.ui.Modifier

inline fun Modifier.ifThen(
    predicate: Boolean,
    block: Modifier.() -> Modifier
): Modifier = if (predicate) {
    block()
} else {
    this
}