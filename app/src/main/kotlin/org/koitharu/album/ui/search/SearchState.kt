package org.koitharu.album.ui.search

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
data class SearchState(
    val textFieldState: TextFieldState,
    val searchBarState: SearchBarState,
    val gridState: LazyGridState,
) {

    val isCollapsed: Boolean
        get() = searchBarState.targetValue == SearchBarValue.Collapsed

    val isExpanded: Boolean
        get() = searchBarState.targetValue == SearchBarValue.Expanded

    suspend fun collapse() {
        searchBarState.animateToCollapsed()
        clearText()
    }

    suspend fun expand() = searchBarState.animateToExpanded()

    fun clearText() = textFieldState.edit {
        replace(0, length, "")
    }
}

@Composable
fun rememberSearchState() = SearchState(
    textFieldState = rememberTextFieldState(),
    searchBarState = rememberSearchBarState(),
    gridState = rememberLazyGridState(),
)