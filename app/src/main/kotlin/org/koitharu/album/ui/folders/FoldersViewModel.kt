package org.koitharu.album.ui.folders

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.model.MediaFolder
import org.koitharu.album.repository.FavoritesRepository
import org.koitharu.album.repository.FoldersRepository
import org.koitharu.album.ui.common.MviViewModel
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: FoldersRepository,
    private val favoritesRepository: FavoritesRepository,
) : MviViewModel<FoldersState, FoldersIntent, Nothing>(FoldersState()) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            repository.observeFolders()
                .map { list ->
                    mapFolders(list)
                }.collect { folders ->
                    state.update {
                        it.copy(items = folders)
                    }
                }
        }
    }

    override fun handleIntent(intent: FoldersIntent) {
        TODO("Not yet implemented")
    }

    private suspend fun mapFolders(list: List<MediaFolder>): PersistentList<FolderItem> =
        buildList(list.size + 2) {
            add(
                FolderItem.Favorites(
                    favoritesRepository.getFavoritesCount(),
                    null,
                )
            )
            add(
                FolderItem.RecycleBin(
                    0,
                    null,
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