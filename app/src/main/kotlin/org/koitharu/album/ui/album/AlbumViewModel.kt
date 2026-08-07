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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.album.model.isSameMonth
import org.koitharu.album.repository.AlbumSource
import org.koitharu.album.repository.FavoritesRepository
import org.koitharu.album.repository.FavoritesSource
import org.koitharu.album.repository.GalleryRepository
import org.koitharu.album.repository.RecycleBinSource
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
    @Assisted private  val folder: FolderItem?,
    private val contentResolver: ContentResolver,
    private val repository: GalleryRepository,
    private val favoritesRepository: FavoritesRepository,
) : MviViewModel<AlbumState, AlbumIntent, Nothing>(AlbumState()) {

    val pagerContent = Pager(
        config = PagingConfig(
            pageSize = 30,
            enablePlaceholders = true,
        ),
        pagingSourceFactory = {
            when (folder) {
                is FolderItem.Favorites -> FavoritesSource(favoritesRepository, contentResolver)
                is FolderItem.RecycleBin -> RecycleBinSource(contentResolver)
                is FolderItem.Bucket,
                null -> AlbumSource(folder?.id, contentResolver)
            }
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
                        repository.getRandomImage()
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