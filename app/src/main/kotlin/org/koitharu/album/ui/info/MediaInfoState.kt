package org.koitharu.album.ui.info

import android.util.Size
import androidx.compose.runtime.Immutable
import org.koitharu.album.ui.common.AlbumItem
import kotlin.math.roundToLong

@Immutable
data class MediaInfoState(
    val fileName: String?,
    val latLng: Pair<Double, Double>?,
    val model: String?,
    val size: Size?,
    val histogram: ImageHistogram?,
) {

    constructor(media: AlbumItem.Media) : this(
        fileName = media.name,
        latLng = null,
        model = null,
        size = null,
        histogram = null,
    )

    val sizeMp: Double? = size?.run {
        val totalPixels = width.toLong() * height.toLong()
        (totalPixels / 100_000.0).roundToLong() / 10.0
    }
}