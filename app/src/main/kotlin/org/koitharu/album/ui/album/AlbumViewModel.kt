package org.koitharu.album.ui.album

import android.content.ContentResolver
import android.net.Uri
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
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koitharu.album.model.HomeBannerSource.NONE
import org.koitharu.album.model.HomeBannerSource.RANDOM
import org.koitharu.album.model.HomeBannerSource.YEAR_AGO
import org.koitharu.album.model.isSameMonth
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.ui.album.AlbumIntent.CancelSelectionMode
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumIntent.HandleClick
import org.koitharu.album.ui.album.AlbumIntent.HandleLongClick
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Delete
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Hide
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Recover
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Share
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Trash
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Unfavorite
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Unhide
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.common.ShellIntegrationHelper
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.info.BitmapAnalyzer
import org.koitharu.album.util.getSelectedItems
import org.koitharu.album.util.runCatchingCancellable
import org.koitharu.album.util.tickerFlow
import org.koitharu.album.util.toggling
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    @Assisted private val folder: FolderItem?,
    private val gallerySourceFactory: GallerySourceFactory,
    private val repository: MediaStoreRepository,
    private val settingsRepository: SettingsRepository,
    private val shellIntegrationHelper: ShellIntegrationHelper,
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val contentResolver: ContentResolver,
) : MviViewModel<AlbumState, AlbumIntent, Nothing>(AlbumState(folder)) {

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
        pagingData.insertSeparators { before, after ->
            when {
                after == null -> null
                before == null ->
                    if (folder != null) {
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
        if (folder == null) {
            observeBanner()
        }
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.gridScale.collect { gridScale ->
                state.update { it.copy(scale = gridScale) }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.useRecycleBin.collect { isRecycleBinEnabled ->
                state.update { it.copy(isRecycleBinEnabled = isRecycleBinEnabled) }
            }
        }
    }

    private fun observeBanner() {
        viewModelScope.launch(Dispatchers.Default) {
            settingsRepository.homeBannerSource.flatMapLatest { bannerSource ->
                if (bannerSource == NONE) {
                    flowOf(null)
                } else {
                    tickerFlow(10.seconds).mapNotNull {
                        runCatchingCancellable {
                            when (bannerSource) {
                                RANDOM -> repository.getRandomMedia(
                                    isImageOnly = true,
                                    isFavoriteOnly = false
                                )

                                YEAR_AGO -> repository.findByDate(
                                    dateTo = yearAgo(minusDays = 0),
                                    dateFrom = yearAgo(minusDays = 30),
                                    limit = 40,
                                ).randomOrNull()

                                NONE -> null
                            }
                        }.onFailure {
                            it.printStackTrace()
                        }.getOrNull()
                    }
                }
            }.mapLatest { image ->
                image to (image?.run { isBannerDark(uri) } == true)
            }.collect { (image, isDark) ->
                state.update {
                    it.copy(
                        banner = image?.let { x -> AlbumItem.Media(x) as? AlbumItem.Image },
                        isBannerDark = isDark,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is HandleClick -> state.update {
                if (it.selectedItems.isEmpty()) {
                    it.copy(openedItem = intent.media)
                } else {
                    it.copy(selectedItems = it.selectedItems.toggling(intent.media.id))
                }
            }

            is HandleLongClick -> state.update {
                it.copy(
                    selectedItems = it.selectedItems.toggling(intent.media.id)
                )
            }

            CloseMedia -> state.update {
                it.copy(openedItem = null)
            }

            is UpdateScale -> viewModelScope.launch(Dispatchers.Default) {
                val newScale = state.updateAndGet {
                    it.copy(scale = (it.scale * intent.factor).coerceIn(1f, 5f))
                }.scale
                settingsRepository.setGridScale(newScale)
            }

            CancelSelectionMode -> state.update {
                it.copy(selectedItems = persistentSetOf())
            }

            is AlbumIntent.SelectionAlbumIntent -> viewModelScope.launch(Dispatchers.Default) {
                val selectedItems = gridContent.getSelectedItems(
                    ids = state.value.selectedItems
                )
                if (handleSelectionIntent(selectedItems = selectedItems, intent = intent)) {
                    handleIntent(CancelSelectionMode)
                }
            }
        }
    }

    private suspend fun handleSelectionIntent(
        selectedItems: Collection<AlbumItem.Media>,
        intent: AlbumIntent.SelectionAlbumIntent,
    ): Boolean = when (intent) {
        Share -> {
            shellIntegrationHelper.shareMedia(selectedItems)
            true
        }

        Delete -> {
            repository.deleteMedia(
                media = selectedItems.map { it.uri },
                useRecycleBin = false,
            )
            true
        }

        Hide -> {
            hiddenMediaRepository.setIsHidden(selectedItems.map { it.id }, true)
            true
        }

        Recover -> {
            repository.recoverMedia(selectedItems.map { it.uri })
            true
        }

        Unfavorite -> {
            repository.setIsFavorite(selectedItems.map { it.uri }, false)
            true
        }

        Unhide -> {
            hiddenMediaRepository.setIsHidden(selectedItems.map { it.id }, false)
            true
        }

        Trash -> {
            repository.deleteMedia(
                media = selectedItems.map { it.uri },
                useRecycleBin = true,
            )
            true
        }
    }

    private fun yearAgo(minusDays: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        calendar.add(Calendar.DAY_OF_MONTH, -minusDays)
        return calendar.timeInMillis
    }

    private suspend fun isBannerDark(image: Uri) = runCatchingCancellable {
        contentResolver.openInputStream(image)?.use {
            BitmapAnalyzer().isTopRegionDark(it)
        } == true
    }.getOrDefault(true)

    @AssistedFactory
    interface Factory {

        fun create(folder: FolderItem?): AlbumViewModel
    }
}