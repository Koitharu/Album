package org.koitharu.album.ui.common

import androidx.compose.runtime.Immutable

@Immutable
data class Fraction(
    val numerator: Int,
    val denominator: Int,
) {

    fun toFloat(): Float = if (isUnspecified()) {
        Float.NaN
    } else {
        numerator.toFloat() / denominator.toFloat()
    }

    override fun toString(): String = if (isUnspecified()) {
        "Unspecified"
    } else {
        "$numerator:$denominator"
    }

    fun isUnspecified() = numerator == 0 || denominator == 0

    companion object {

        val Unspecified
            get() = Fraction(0, 0)
    }
}