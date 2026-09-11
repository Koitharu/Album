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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.repository.ExifEditor
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.common.ShellIntegrationHelper
import org.koitharu.album.ui.viewer.ViewerEffect.CloseViewer
import org.koitharu.album.ui.viewer.ViewerEffect.OnError
import org.koitharu.album.ui.viewer.ViewerIntent.CloseInfo
import org.koitharu.album.ui.viewer.ViewerIntent.Delete
import org.koitharu.album.ui.viewer.ViewerIntent.Edit
import org.koitharu.album.ui.viewer.ViewerIntent.Favorite
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.album.ui.viewer.ViewerIntent.OpenInExternalApp
import org.koitharu.album.ui.viewer.ViewerIntent.OpenInfo
import org.koitharu.album.ui.viewer.ViewerIntent.Print
import org.koitharu.album.ui.viewer.ViewerIntent.Recover
import org.koitharu.album.ui.viewer.ViewerIntent.Rotate
import org.koitharu.album.ui.viewer.ViewerIntent.Share
import org.koitharu.album.ui.viewer.ViewerIntent.UseAs
import org.koitharu.album.util.printStackTraceDebug
import org.koitharu.album.util.runCatchingCancellable

@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel @AssistedInject constructor(
    @Assisted media: AlbumItem.Media,
    private val repository: MediaStoreRepository,
    private val settingsRepository: SettingsRepository,
    private val shellIntegrationHelper: ShellIntegrationHelper,
    private val editorFactory: ExifEditor.Factory,
) : MviViewModel<ViewerState, ViewerIntent, ViewerEffect>(ViewerState(media)) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            state.map {
                it.currentMedia
            }.distinctUntilChangedBy { it.id }
                .flatMapLatest { currentMedia ->
                    repository.observeIsFavorite(currentMedia.id)
                        .map { currentMedia.copyWithFavoriteState(it) }
                        .catch { e ->
                            e.printStackTraceDebug()
                            sendEffect(OnError(e))
                        }
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
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.isRotationGestureEnabled.collect { allowRotation ->
                state.update { it.copy(isRotationGestureEnabled = allowRotation) }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.isVideMutedOnStart.collect { muted ->
                state.update { it.copy(isVideosMutedOnStart = muted) }
            }
        }
    }

    override fun handleIntent(intent: ViewerIntent) {
        when (intent) {
            is OnMediaChanged -> viewModelScope.launch(Dispatchers.Default) {
                if (intent.media == null) {
                    sendEffect(CloseViewer)
                } else {
                    state.update {
                        it.copy(currentMedia = intent.media)
                    }
                }
            }

            is Favorite -> setIsFavorite(
                intent.media,
                intent.isFavorite,
            )

            is Delete -> delete(intent.media)
            is Recover -> recover(intent.media)
            is Rotate -> viewModelScope.launch {
                runCatchingCancellable {
                    editorFactory.create(intent.image.uri)
                        .rotate(intent.angle)
                        .commit()
                }.onFailure { e ->
                    e.printStackTraceDebug()
                    sendEffect(OnError(e))
                }
            }

            is Print -> viewModelScope.launch {
                shellIntegrationHelper.print(intent.image)
            }

            is Share -> viewModelScope.launch {
                shellIntegrationHelper.shareMedia(intent.media)
            }

            is UseAs -> viewModelScope.launch {
                shellIntegrationHelper.openUseAs(intent.image)
            }

            is Edit -> openEditor(intent.image)
            CloseInfo -> state.update {
                it.copy(
                    infoBottomSheetImage = null,
                )
            }

            is OpenInfo -> state.update {
                it.copy(
                    infoBottomSheetImage = intent.image,
                )
            }

            is OpenInExternalApp -> viewModelScope.launch {
                shellIntegrationHelper.openInExternalApp(intent.media)
            }
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

    private fun openEditor(image: AlbumItem.Image) {
        viewModelScope.launch {
            if (settingsRepository.useExternalEditor.first()) {
                shellIntegrationHelper.openImageEditor(image)
            } else {
                sendEffect(ViewerEffect.OpenImageEditor(image.uri, image.name))
            }
        }
    }

    private fun delete(
        media: AlbumItem.Media,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatchingCancellable {
                val useRecycleBin = !media.isTrashed &&
                        Features.isRecycleBinSupported &&
                        settingsRepository.useRecycleBin.first()
                repository.deleteMedia(listOf(media.uri), useRecycleBin)
            }.onFailure {
                it.printStackTraceDebug()
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
                it.printStackTraceDebug()
                sendEffect(OnError(it))
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(media: AlbumItem.Media): ViewerViewModel
    }
}