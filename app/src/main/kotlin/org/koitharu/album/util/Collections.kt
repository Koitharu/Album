package org.koitharu.album.util

import kotlinx.collections.immutable.PersistentSet

fun <T> PersistentSet<T>.toggling(item: T): PersistentSet<T> = if (item in this) {
    removing(item)
} else {
    adding(item)
}