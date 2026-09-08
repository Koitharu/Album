package org.koitharu.album.ui.picker

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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.album.model.isSameMonth
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.repository.pagingsource.PickerSource
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.picker.PickerEffect.OnError
import org.koitharu.album.ui.picker.PickerEffect.ReturnMultipleItems
import org.koitharu.album.ui.picker.PickerEffect.ReturnSingleItem
import org.koitharu.album.ui.picker.PickerIntent.OnDoneClick
import org.koitharu.album.ui.picker.PickerIntent.OnItemClick
import org.koitharu.album.ui.picker.PickerIntent.SetBucketId
import org.koitharu.album.ui.picker.PickerIntent.ToggleFavoriteOnly
import org.koitharu.album.ui.picker.PickerIntent.ToggleImagesOnly
import org.koitharu.album.ui.picker.PickerIntent.ToggleVideosOnly
import org.koitharu.album.ui.picker.PickerIntent.UpdateScale
import org.koitharu.album.util.getSelectedItems
import org.koitharu.album.util.toggling

@HiltViewModel(assistedFactory = PickerViewModel.Factory::class)
class PickerViewModel @AssistedInject constructor(
    @Assisted private val options: PickerOptions,
    private val settingsRepository: SettingsRepository,
    private val sourceFactory: PickerSource.Factory,
    private val mediaStoreRepository: MediaStoreRepository,
) : MviViewModel<PickerState, PickerIntent, PickerEffect>(
    PickerState()
) {

    val content: Flow<PagingData<AlbumItem>> = state.map {
        PickerFilters(
            bucketId = it.folder?.id,
            isFavoritesOnly = it.isFavoritesOnly,
            isImagesOnly = it.isImagesOnly,
            isVideosOnly = it.isVideosOnly,
        )
    }.distinctUntilChanged()
        .flatMapLatest { filters ->
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    enablePlaceholders = true,
                ),
                pagingSourceFactory = {
                    sourceFactory.create(
                        includeVideos = options.isVideosAllowed && !filters.isImagesOnly,
                        includeImages = options.isImagesAllowed && !filters.isVideosOnly,
                        isFavoriteOnly = filters.isFavoritesOnly,
                        bucketId = filters.bucketId,
                    )
                }
            ).flow
        }.map { pagingData ->
            pagingData.map { mediaItem ->
                AlbumItem.Media(mediaItem)
            }.insertSeparators { before, after ->
                when {
                    after == null -> null
                    before == null ->
                        if (state.value.folder != null) {
                            AlbumItem.DateHeader(after.dateAdded)
                        } else {
                            null
                        }

                    !isSameMonth(
                        before.dateAdded,
                        after.dateAdded
                    ) -> AlbumItem.DateHeader(after.dateAdded)

                    else -> null
                }
            }
        }.cachedIn(viewModelScope + Dispatchers.Default)

    init {
        observeFolders()
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.gridScale.collect { gridScale ->
                state.update { it.copy(scale = gridScale) }
            }
        }
    }

    override fun handleIntent(intent: PickerIntent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (intent) {
                OnDoneClick -> {
                    val selectedItems = content.getSelectedItems(
                        ids = state.value.selectedItems
                    )
                    if (options.isMultipleChoice) {
                        if (selectedItems.isNotEmpty()) {
                            sendEffect(ReturnMultipleItems(selectedItems))
                        }
                    } else {
                        selectedItems.singleOrNull()?.let {
                            sendEffect(ReturnSingleItem(it))
                        }
                    }
                }

                is OnItemClick -> if (options.isMultipleChoice) {
                    state.update {
                        it.copy(selectedItems = it.selectedItems.toggling(intent.item.id))
                    }
                } else {
                    sendEffect(ReturnSingleItem(intent.item))
                }

                is UpdateScale -> {
                    val newScale = state.updateAndGet {
                        it.copy(scale = (it.scale * intent.factor).coerceIn(1f, 5f))
                    }.scale
                    settingsRepository.setGridScale(newScale)
                }

                ToggleFavoriteOnly -> state.update {
                    it.copy(isFavoritesOnly = !it.isFavoritesOnly)
                }

                ToggleImagesOnly -> state.update {
                    it.copy(isImagesOnly = !it.isImagesOnly)
                }

                ToggleVideosOnly -> state.update {
                    it.copy(isVideosOnly = !it.isVideosOnly)
                }

                is SetBucketId -> state.update {
                    it.copy(
                        folder = if (intent.bucketId == null) {
                            null
                        } else {
                            it.availableFolders.find { x -> x.id == intent.bucketId }
                        }
                    )
                }
            }
        }
    }

    private fun observeFolders() = viewModelScope.launch(Dispatchers.Default) {
        mediaStoreRepository.observeFolders().map { folders ->
            folders.map {
                FolderItem.Bucket(
                    id = it.id,
                    name = it.name,
                    size = it.size,
                    thumbnail = it.thumbnail,
                )
            }
        }.catch {
            sendEffect(OnError(it))
        }.collect { buckets ->
            state.update {
                it.copy(availableFolders = buckets.toImmutableList())
            }
        }
    }

    private data class PickerFilters(
        val bucketId: String?,
        val isFavoritesOnly: Boolean,
        val isImagesOnly: Boolean,
        val isVideosOnly: Boolean,
    )

    @AssistedFactory
    interface Factory {

        fun create(options: PickerOptions): PickerViewModel
    }
}