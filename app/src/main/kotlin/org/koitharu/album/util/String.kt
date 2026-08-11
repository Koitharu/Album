package org.koitharu.album.util

import java.util.Locale

fun String.toTitleCase() = replaceFirstChar {
    it.uppercase(Locale.getDefault())
}