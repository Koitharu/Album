package org.koitharu.album.ui.album

import android.content.ContentResolver
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koitharu.album.model.MediaItem
import org.koitharu.album.repository.AlbumSource
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumIntent.OpenMedia
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.MviViewModel
import kotlin.time.Instant

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    @Assisted val albumId: String?,
    val contentResolver: ContentResolver,
) : MviViewModel<AlbumState, AlbumIntent, Nothing>(AlbumState()) {

    val pagerContent = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = true,
            jumpThreshold = 40,
        ),
        pagingSourceFactory = {
            AlbumSource(contentResolver)
        }
    ).flow.map { pagingData ->
        pagingData.map<MediaItem, AlbumItem.Media> { mediaItem ->
            AlbumItem.Image(
                index = mediaItem.index,
                id = mediaItem.id,
                uri = mediaItem.uri,
                thumbnail = mediaItem.thumbnail,
                name = mediaItem.name,
                dateAdded = Instant.fromEpochSeconds(mediaItem.dateAdded)
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
            )
        }
    }.cachedIn(viewModelScope + Dispatchers.Default)

    val gridContent: Flow<PagingData<AlbumItem>> = pagerContent.map { pagingData ->
        pagingData.insertSeparators<AlbumItem.Media, AlbumItem> { before, after ->
            if (before == null || after == null) {
                null
            } else if (before.dateAdded.month != after.dateAdded.month) {
                AlbumItem.DateHeader(
                    after.dateAdded.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                )
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope + Dispatchers.Default)

    override fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is OpenMedia -> state.update {
                it.copy(openedItem = intent.media)
            }

            CloseMedia -> state.update {
                it.copy(openedItem = null)
            }

            is UpdateScale -> state.update {
                it.copy(scale = (it.scale * intent.factor).coerceIn(1f, 5f))
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(albumId: String?): AlbumViewModel
    }
}