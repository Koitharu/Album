package org.koitharu.album.ui.viewer

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import org.koitharu.album.ui.album.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged

@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel @AssistedInject constructor(
    @Assisted media: AlbumItem.Media,
) : MviViewModel<ViewerState, ViewerIntent, Nothing>(ViewerState(media)) {

    override fun handleIntent(intent: ViewerIntent) {
        when (intent) {
            is OnMediaChanged -> state.update {
                it.copy(currentMedia = intent.media)
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(media: AlbumItem.Media): ViewerViewModel
    }
}