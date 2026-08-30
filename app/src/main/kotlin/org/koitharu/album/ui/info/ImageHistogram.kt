package org.koitharu.album.ui.info

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

const val HISTOGRAM_SIZE = 256

@Immutable
class HistogramData(
    private val data: IntArray,
    @Stable val maxValue: Int,
) : RandomAccess {

    @Stable
    val size: Int = HISTOGRAM_SIZE

    @Stable
    operator fun get(index: Int) = data.getOrElse(index) { 0 }

    @Stable
    val isEmpty = data.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int = data.contentHashCode()

    companion object {

        val Empty = HistogramData(intArrayOf(), 0)
    }
}

@Immutable
data class ImageHistogram(
    val red: HistogramData,
    val green: HistogramData,
    val blue: HistogramData,
    val luminance: HistogramData,
)