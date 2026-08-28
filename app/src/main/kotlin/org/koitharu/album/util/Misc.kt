package org.koitharu.album.util

inline fun <A, B, R> lets(a: A?, b: B?, block: (A, B) -> R): R? = if (a != null && b != null) {
    block(a, b)
} else {
    null
}