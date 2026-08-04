package org.koitharu.album.ui.util

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@Composable
fun formattedDateTime(
    dateTime: LocalDateTime,
    flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
): String {
    val context = LocalContext.current
    return remember(dateTime, flags) {
        val millis = dateTime.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        DateUtils.formatDateTime(context, millis, flags)
    }
}