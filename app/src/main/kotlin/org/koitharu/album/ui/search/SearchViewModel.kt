package org.koitharu.album.ui.search

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import org.koitharu.album.repository.pagingsource.SearchSource
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.search.SearchIntent.DoSearch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceFactory: SearchSource.Factory,
) : MviViewModel<Any, SearchIntent, Nothing>(Any()) {

    private val currentQuery = MutableStateFlow("")

    val searchResults = currentQuery
        .debounce { value ->
            if (value.length < 3) 0.milliseconds else 500.milliseconds
        }.flatMapLatest { query ->
            if (query.length < 3) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 10,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = {
                        sourceFactory.create(query)
                    }
                ).flow
            }
        }.map { pagingData ->
            pagingData.map { mediaItem ->
                AlbumItem.Media(mediaItem)
            }
        }.cachedIn(viewModelScope + Dispatchers.Default)

    override fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is DoSearch -> currentQuery.value = intent.query
        }
    }
}