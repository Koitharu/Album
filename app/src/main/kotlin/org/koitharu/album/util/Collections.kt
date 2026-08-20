package org.koitharu.album.util

import kotlinx.collections.immutable.PersistentSet

fun <T> PersistentSet<T>.toggling(item: T): PersistentSet<T> = if (item in this) {
    removing(item)
} else {
    adding(item)
}

public inline fun <T, R : Any> List<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this.asReversed()) {
        val result = transform(element)
        if (result != null) {
            return result
        }
    }
    return null
}