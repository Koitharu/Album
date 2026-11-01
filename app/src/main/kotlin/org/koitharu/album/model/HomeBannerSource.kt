package org.koitharu.album.model

import java.util.Calendar

enum class HomeBannerSource {
    NONE, RANDOM, YEAR_AGO, FAVORITES;

    fun toMediaQuery(): MediaQuery? = when (this) {
        NONE -> null
        RANDOM -> MediaQuery(
            includeVideos = false,
        )

        YEAR_AGO -> MediaQuery(
            includeVideos = false,
            dateRange = yearAgo(30)..yearAgo(0),
        )

        FAVORITES -> MediaQuery(
            includeVideos = false,
            isFavorites = true,
        )
    }

    private fun yearAgo(minusDays: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        calendar.add(Calendar.DAY_OF_MONTH, -minusDays)
        return calendar.timeInMillis
    }
}