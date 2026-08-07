package org.koitharu.album.ui.viewer

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.repository.FavoritesRepository
import org.koitharu.album.repository.GalleryRepository
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.viewer.ViewerEffect.OnError
import org.koitharu.album.ui.viewer.ViewerIntent.Favorite
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.album.util.runCatchingCancellable

@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel @AssistedInject constructor(
    @Assisted media: AlbumItem.Media,
    private val repository: GalleryRepository,
    private val favoritesRepository: FavoritesRepository,
) : MviViewModel<ViewerState, ViewerIntent, ViewerEffect>(ViewerState(media)) {

    override fun handleIntent(intent: ViewerIntent) {
        when (intent) {
            is OnMediaChanged -> viewModelScope.launch(Dispatchers.Default) {
                state.update {
                    it.copy(
                        currentMedia = intent.media,
                        isFavorite = favoritesRepository.isFavorite(intent.media.id),
                    )
                }
            }

            is Favorite -> setIsFavorite(
                intent.media,
                intent.isFavorite,
            )
        }
    }

    private fun setIsFavorite(
        media: AlbumItem.Media,
        isFavorite: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                favoritesRepository.setIsFavorite(media.id, isFavorite)
            }.onFailure {
                sendEffect(OnError(it))
            }.onSuccess {
                state.update {
                    it.copy(
                        isFavorite = if (media == it.currentMedia) {
                            isFavorite
                        } else {
                            it.isFavorite
                        }
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(media: AlbumItem.Media): ViewerViewModel
    }
}