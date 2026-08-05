package org.koitharu.album.ui.util

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koitharu.album.model.ImmutableDateTime

@Composable
fun formattedDateTime(
    dateTime: ImmutableDateTime,
    flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
): String {
    val context = LocalContext.current
    return remember(dateTime, flags) {
        DateUtils.formatDateTime(context, dateTime.millis, flags)
    }
}