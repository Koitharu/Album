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

public fun Int.formatTimeSeconds(): String {
    if (this == 0) {
        return "00:00"
    }
    val sec = this / 1000
    val hours = sec / 3600
    val minutes = (sec % 3600) / 60
    val seconds = sec % 60

    return if (hours == 0) {
        "%02d:%02d".format(minutes, seconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}