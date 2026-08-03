package org.koitharu.album.ui.gallery

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koitharu.album.repository.AlbumSource
import javax.inject.Inject
import kotlin.time.Instant

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val contentResolver: ContentResolver,
) : ViewModel() {

    val content: Flow<PagingData<GalleryItem>> = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as pageSize and enabling or disabling placeholders.
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            AlbumSource(contentResolver)
        }
    ).flow.map { pagingData ->
        pagingData.map { mediaItem ->
            GalleryItem.Image(
                id = mediaItem.id,
                uri = mediaItem.uri,
                thumbnail = mediaItem.thumbnail,
                name = mediaItem.name,
                dateAdded = Instant.fromEpochSeconds(mediaItem.dateAdded)
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
            )
        }.insertSeparators<GalleryItem.Image, GalleryItem> { before, after ->
            if (before == null || after == null) {
                null
            } else if (before.dateAdded.month != after.dateAdded.month) {
                GalleryItem.DateHeader(after.dateAdded.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope + Dispatchers.Default)

}