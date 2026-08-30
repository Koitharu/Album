package org.koitharu.album.ui.info

import android.content.ContentResolver
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.util.lets
import org.koitharu.album.util.runCatchingCancellable

@HiltViewModel(assistedFactory = MediaInfoViewModel.Factory::class)
class MediaInfoViewModel @AssistedInject constructor(
    @Assisted private val media: AlbumItem.Image,
    private val contentResolver: ContentResolver,
) : MviViewModel<MediaInfoState, Nothing, Nothing>(MediaInfoState(media)) {

    init {
        readExifData()
        readHistogram()
    }

    override fun handleIntent(intent: Nothing) {

    }

    private fun readHistogram() = viewModelScope.launch(Dispatchers.Default) {
        val histogram = runCatchingCancellable {
            contentResolver.openInputStream(media.uri)?.use { stream ->
                HistogramReader().decodeHistogram(stream)
            }
        }.getOrNull()
        state.update {
            it.copy(histogram = histogram)
        }
    }

    private fun readExifData() = viewModelScope.launch(Dispatchers.Default) {
        runCatchingCancellable {
            contentResolver.openInputStream(media.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                state.update {
                    it.copy(
                        latLng = exif.latLong?.let { latLng -> latLng[0] to latLng[1] },
                        model = exif.getAttribute(ExifInterface.TAG_MODEL),
                        size = lets(
                            exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull(),
                            exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull(),
                        ) { w, h ->
                            Size(w, h)
                        }
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(media: AlbumItem.Image): MediaInfoViewModel
    }
}