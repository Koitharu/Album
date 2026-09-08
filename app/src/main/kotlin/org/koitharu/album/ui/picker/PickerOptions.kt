package org.koitharu.album.ui.picker

import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class PickerOptions(
    val isMultipleChoice: Boolean,
    val isVideosAllowed: Boolean,
    val isImagesAllowed: Boolean,
) {

    @Stable
    val isAllMediaAllowed: Boolean
        get() = isImagesAllowed && isVideosAllowed

    companion object {

        fun from(intent: Intent): PickerOptions {
            val typeRequested = intent.type?.split('/')
            val exactType = if (typeRequested?.size == 2) {
                typeRequested[0].takeUnless { it == "*" }
            } else {
                null
            }
            return PickerOptions(
                isMultipleChoice = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false),
                isVideosAllowed = exactType == null || exactType == "video",
                isImagesAllowed = exactType == null || exactType == "image",
            )
        }
    }
}
