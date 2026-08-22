package org.koitharu.album.util

import java.util.Locale

fun String.toTitleCase() = replaceFirstChar {
    it.uppercase(Locale.getDefault())
}

fun StringBuilder.appendIfNotEmpty(what: String): StringBuilder = if (isNotEmpty()) {
    append(what)
} else {
    this
}