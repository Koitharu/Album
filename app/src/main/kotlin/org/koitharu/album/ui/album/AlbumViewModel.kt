package org.koitharu.album.ui.album

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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.album.model.isSameMonth
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumIntent.OpenMedia
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.util.runCatchingCancellable
import org.koitharu.album.util.tickerFlow
import kotlin.time.Duration.Companion.seconds

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    @Assisted private val folder: FolderItem?,
    private val gallerySourceFactory: GallerySourceFactory,
    private val repository: MediaStoreRepository,
) : MviViewModel<AlbumState, AlbumIntent, Nothing>(AlbumState()) {

    val pagerContent = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = true,
        ),
        pagingSourceFactory = {
            gallerySourceFactory.create(folder)
        }
    ).flow.map { pagingData ->
        pagingData.map { mediaItem ->
            AlbumItem.Media(mediaItem)
        }
    }.cachedIn(viewModelScope + Dispatchers.Default)

    val gridContent: Flow<PagingData<AlbumItem>> = pagerContent.map { pagingData ->
        pagingData.insertSeparators<AlbumItem.Media, AlbumItem> { before, after ->
            if (before == null || after == null) {
                null
            } else if (!isSameMonth(before.dateAdded, after.dateAdded)) {
                AlbumItem.DateHeader(after.dateAdded)
            } else {
                null
            }
        }
    }.cachedIn(viewModelScope + Dispatchers.Default)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            tickerFlow(10.seconds)
                .mapNotNull {
                    runCatchingCancellable {
                        repository.getRandomMedia(isImageOnly = true, isFavoriteOnly = false)
                    }.onFailure {
                        it.printStackTrace()
                    }.getOrNull()
                }.collect { image ->
                    state.update {
                        it.copy(banner = AlbumItem.Media(image) as AlbumItem.Image)
                    }
                }
        }
    }

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

        fun create(folder: FolderItem?): AlbumViewModel
    }
}