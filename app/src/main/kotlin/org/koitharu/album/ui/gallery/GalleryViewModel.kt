package org.koitharu.album.ui.gallery

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.model.isSameMonth
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.repository.pagingsource.GallerySource
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.LocalSharedViewModelStoreOwner
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.common.ShellIntegrationHelper
import org.koitharu.album.ui.gallery.GalleryEffect.OpenMedia
import org.koitharu.album.ui.gallery.GalleryEffect.ShowError
import org.koitharu.album.ui.gallery.GalleryIntent.CancelSelectionMode
import org.koitharu.album.ui.gallery.GalleryIntent.ClearRecycleBin
import org.koitharu.album.ui.gallery.GalleryIntent.HandleClick
import org.koitharu.album.ui.gallery.GalleryIntent.HandleLongClick
import org.koitharu.album.ui.gallery.GalleryIntent.OpenCamera
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Delete
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Hide
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Recover
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Share
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Trash
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Unfavorite
import org.koitharu.album.ui.gallery.GalleryIntent.SelectedMediaIntent.Unhide
import org.koitharu.album.ui.gallery.GalleryIntent.UpdateScale
import org.koitharu.album.ui.gallery.GalleryIntent.UpdateSearchQuery
import org.koitharu.album.ui.info.BitmapAnalyzer
import org.koitharu.album.util.getSelectedItems
import org.koitharu.album.util.printStackTraceDebug
import org.koitharu.album.util.runCatchingCancellable
import org.koitharu.album.util.tickerFlow
import org.koitharu.album.util.toggling
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

@HiltViewModel(assistedFactory = GalleryViewModel.Factory::class)
class GalleryViewModel @AssistedInject constructor(
    @Assisted query: MediaQuery,
    private val gallerySourceFactory: GallerySource.Factory,
    private val repository: MediaStoreRepository,
    private val settingsRepository: SettingsRepository,
    private val shellIntegrationHelper: ShellIntegrationHelper,
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val contentResolver: ContentResolver,
) : MviViewModel<GalleryState, GalleryIntent, GalleryEffect>(GalleryState(query)) {

    private val isCommonAlbum = query == MediaQuery.DEFAULT

    val pagerContent = state.map {
        it.query
    }.distinctUntilChanged()
        .flatMapLatest { mediaQuery ->
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    enablePlaceholders = true,
                ),
                pagingSourceFactory = {
                    gallerySourceFactory.create(mediaQuery)
                }
            ).flow
        }.map { pagingData ->
            pagingData.map { mediaItem ->
                AlbumItem.Media(mediaItem)
            }
        }.cachedIn(viewModelScope + Dispatchers.Default)

    val gridContent: Flow<PagingData<AlbumItem>> = pagerContent.map { pagingData ->
        pagingData.insertSeparators { before, after ->
            when {
                after == null -> null
                before == null ->
                    if (!isCommonAlbum) {
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
        if (isCommonAlbum) {
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
                                    isFavoriteOnly = false,
                                    excludeHidden = true,
                                )

                                YEAR_AGO -> repository.findByDate(
                                    dateRange = yearAgo(30)..yearAgo(0),
                                    limit = 40,
                                ).randomOrNull()

                                NONE -> null
                            }
                        }.onFailure {
                            it.printStackTraceDebug()
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

    override fun handleIntent(intent: GalleryIntent) {
        when (intent) {
            is HandleClick -> state.update {
                if (it.selectedItems.isEmpty()) {
                    sendEffectAsync(OpenMedia(intent.media))
                    it
                } else {
                    it.copy(selectedItems = it.selectedItems.toggling(intent.media.id))
                }
            }

            is HandleLongClick -> state.update {
                it.copy(
                    selectedItems = it.selectedItems.toggling(intent.media.id)
                )
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

            is SelectedMediaIntent -> viewModelScope.launch(Dispatchers.Default) {
                val selectedItems = gridContent.getSelectedItems(
                    ids = state.value.selectedItems
                )
                runCatchingCancellable {
                    handleSelectionIntent(selectedItems = selectedItems, intent = intent)
                }.onSuccess {
                    handleIntent(CancelSelectionMode)
                }.onFailure { e ->
                    e.printStackTraceDebug()
                    sendEffect(ShowError(e))
                }
            }

            ClearRecycleBin -> viewModelScope.launch(Dispatchers.Default) {
                runCatchingCancellable {
                    repository.clearRecycleBin()
                }.onFailure { e ->
                    e.printStackTraceDebug()
                    sendEffect(ShowError(e))
                }
            }

            OpenCamera -> viewModelScope.launch {
                shellIntegrationHelper.openCamera()
            }

            is UpdateSearchQuery -> state.update {
                it.copy(
                    query = it.query.copy(
                        searchQuery = intent.value,
                    ),
                )
            }
        }
    }

    private suspend fun handleSelectionIntent(
        selectedItems: Collection<AlbumItem.Media>,
        intent: SelectedMediaIntent,
    ): Unit = when (intent) {
        Share -> shellIntegrationHelper.shareMedia(selectedItems)

        Delete -> repository.deleteMedia(
            media = selectedItems.map { it.uri },
            useRecycleBin = false,
        )

        Hide -> hiddenMediaRepository.setIsHidden(selectedItems.map { it.id }, true)

        Recover -> repository.recoverMedia(selectedItems.map { it.uri })

        Unfavorite -> repository.setIsFavorite(selectedItems.map { it.uri }, false)

        Unhide -> hiddenMediaRepository.setIsHidden(selectedItems.map { it.id }, false)

        Trash -> repository.deleteMedia(
            media = selectedItems.map { it.uri },
            useRecycleBin = true,
        )
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
    }.onFailure { e ->
        e.printStackTraceDebug()
    }.getOrDefault(true)

    @AssistedFactory
    interface Factory {

        fun create(query: MediaQuery): GalleryViewModel
    }
}

@Composable
fun galleryViewModel(query: MediaQuery): GalleryViewModel {
    return hiltViewModel<GalleryViewModel, GalleryViewModel.Factory>(
        viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
            ?: LocalViewModelStoreOwner.current
            ?: error("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"),
        key = query.stringKey()
    ) {
        it.create(query)
    }
}