package org.koitharu.album.model

import android.text.format.DateFormat
import androidx.compose.runtime.Immutable
import java.util.concurrent.TimeUnit

@JvmInline
@Immutable
value class ImmutableDateTime(
    val millis: Long,
) {

    val seconds: Long
        get() = TimeUnit.MILLISECONDS.toSeconds(millis)

    companion object {

        fun ofSeconds(seconds: Long) = ImmutableDateTime(TimeUnit.SECONDS.toMillis(seconds))

        fun now() = ImmutableDateTime(System.currentTimeMillis())
    }
}

fun ImmutableDateTime.format(pattern: String): String {
    return DateFormat.format(pattern, millis).toString()
}

fun isSameMonth(date1: ImmutableDateTime, date2: ImmutableDateTime): Boolean {
    val diffDays = (date1.millis - date2.millis) / 86400000L
    if (diffDays == 0L) return true
    if (diffDays > 31L || diffDays < -31L) return false

    val days1 = date1.millis / 86400000L + 719468L
    val era1 = (if (days1 >= 0) days1 else days1 - 146096L) / 146097L
    val doe1 = days1 - era1 * 146097L
    val yoe1 = (doe1 - doe1 / 1460L + doe1 / 36524L - doe1 / 146096L) / 365L
    val doy1 = doe1 - (365L * yoe1 + yoe1 / 4L - yoe1 / 100L)
    val mp1 = (doy1 * 5L + 2L) / 153L
    val year1 = yoe1 + era1 * 400L + (if (mp1 >= 10) 1 else 0)
    val month1 = if (mp1 < 10) mp1 + 3 else mp1 - 9

    val days2 = date2.millis / 86400000L + 719468L
    val era2 = (if (days2 >= 0) days2 else days2 - 146096L) / 146097L
    val doe2 = days2 - era2 * 146097L
    val yoe2 = (doe2 - doe2 / 1460L + doe2 / 36524L - doe2 / 146096L) / 365L
    val doy2 = doe2 - (365L * yoe2 + yoe2 / 4L - yoe2 / 100L)
    val mp2 = (doy2 * 5L + 2L) / 153L
    val year2 = yoe2 + era2 * 400L + (if (mp2 >= 10) 1 else 0)
    val month2 = if (mp2 < 10) mp2 + 3 else mp2 - 9

    return year1 == year2 && month1 == month2
}