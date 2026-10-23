package org.koitharu.album.util

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

fun <T> PersistentSet<T>.toggling(item: T): PersistentSet<T> = if (item in this) {
    removing(item)
} else {
    adding(item)
}

inline fun <T, R : Any> List<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this.asReversed()) {
        val result = transform(element)
        if (result != null) {
            return result
        }
    }
    return null
}

fun <T : Any> persistentSetOfNotNull(e: T?): PersistentSet<T> = if (e != null) {
    persistentSetOf(e)
} else {
    persistentSetOf()
}

fun <T> MutableList<T>.setLast(value: T) {
    val index = lastIndex
    if (index < 0) {
        add(value)
    } else {
        set(index, value)
    }
}