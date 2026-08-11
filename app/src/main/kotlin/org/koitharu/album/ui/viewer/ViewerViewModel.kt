package org.koitharu.album.ui.viewer

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.viewer.ViewerEffect.OnError
import org.koitharu.album.ui.viewer.ViewerIntent.Favorite
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.album.util.runCatchingCancellable

@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel @AssistedInject constructor(
    @Assisted media: AlbumItem.Media,
    private val repository: MediaStoreRepository,
) : MviViewModel<ViewerState, ViewerIntent, ViewerEffect>(ViewerState(media)) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            state.map {
                it.currentMedia
            }.distinctUntilChangedBy { it.id }
                .flatMapLatest { currentMedia ->
                    repository.observeIsFavorite(currentMedia.id)
                        .map { currentMedia.copyWithFavoriteState(it) }
                        .catch { sendEffect(OnError(it)) }
                }.collectLatest { updatedMedia ->
                    state.update { prevState ->
                        if (prevState.currentMedia.id == updatedMedia.id) {
                            prevState.copy(
                                currentMedia = updatedMedia
                            )
                        } else {
                            prevState
                        }
                    }
                }
        }
    }

    override fun handleIntent(intent: ViewerIntent) {
        when (intent) {
            is OnMediaChanged -> viewModelScope.launch(Dispatchers.Default) {
                state.update {
                    it.copy(currentMedia = intent.media)
                }
            }

            is Favorite -> setIsFavorite(
                intent.media,
                intent.isFavorite,
            )

            is ViewerIntent.Delete -> delete(intent.media)
            is ViewerIntent.Recover -> recover(intent.media)
        }
    }

    private fun setIsFavorite(
        media: AlbumItem.Media,
        isFavorite: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                repository.setIsFavorite(listOf(media.uri), isFavorite)
            }.onFailure {
                sendEffect(OnError(it))
            }
        }
    }

    private fun delete(
        media: AlbumItem.Media,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                repository.deleteMedia(listOf(media.uri))
            }.onFailure {
                sendEffect(OnError(it))
            }
        }
    }

    private fun recover(
        media: AlbumItem.Media,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                repository.recoverMedia(listOf(media.uri))
            }.onFailure {
                sendEffect(OnError(it))
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(media: AlbumItem.Media): ViewerViewModel
    }
}