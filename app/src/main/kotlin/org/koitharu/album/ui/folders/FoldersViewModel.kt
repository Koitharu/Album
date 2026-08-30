package org.koitharu.album.ui.folders

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.model.MediaFolder
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.ui.common.MviViewModel
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: MediaStoreRepository,
    private val hiddenMediaRepository: HiddenMediaRepository,
) : MviViewModel<FoldersState, FoldersIntent, Nothing>(FoldersState()) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                repository.observeFolders(),
                repository.observeFavoritesSize(),
                hiddenMediaRepository.observeHiddenCount(),
            ) { list, favoritesCount, hiddenCount ->
                mapFolders(
                    list = list,
                    favoritesCount = favoritesCount,
                    hiddenCount = hiddenCount,
                )
            }.catch { e ->
                state.update {
                    it.copy(error = e)
                }
            }.collect { folders ->
                state.update {
                    it.copy(items = folders, error = null)
                }
            }
        }
    }

    override fun handleIntent(intent: FoldersIntent) {

    }

    private suspend fun mapFolders(
        list: List<MediaFolder>,
        favoritesCount: Int,
        hiddenCount: Int,
    ): PersistentList<FolderItem> =
        buildList(list.size + 2) {
            add(
                FolderItem.Favorites(
                    size = favoritesCount,
                    thumbnail = null,
                )
            )
            if (Features.isRecycleBinSupported) {
                add(
                    FolderItem.RecycleBin(
                        size = repository.getRecycleBinSize(),
                        thumbnail = null,
                    )
                )
            }
            add(
                FolderItem.Photos(
                    size = repository.getPhotosCount(),
                    thumbnail = null,
                )
            )
            add(
                FolderItem.Videos(
                    size = repository.getVideosCount(),
                    thumbnail = null,
                )
            )
            add(
                FolderItem.Hidden(
                    size = hiddenCount,
                    thumbnail = null,
                )
            )
            list.mapTo(this) {
                FolderItem.Bucket(
                    id = it.id,
                    name = it.name,
                    size = it.size,
                    thumbnail = it.thumbnail,
                )
            }
        }.toPersistentList()
}