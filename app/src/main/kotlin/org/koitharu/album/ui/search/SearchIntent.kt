package org.koitharu.album.ui.search

sealed interface SearchIntent {

    data class DoSearch(
        val query: String,
    ): SearchIntent
}