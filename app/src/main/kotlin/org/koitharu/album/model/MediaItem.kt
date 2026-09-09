package org.koitharu.album.model

import android.net.Uri

data class MediaItem(
    val index: Int,
    val id: Long,
    val name: String?,
    val mimeType: String,
    val uri: Uri,
    val dateAdded: Long,
    val dateModified: Long,
    val isFavorite: Boolean,
    val isTrashed: Boolean,
    val isVideo: Boolean,
    private val path: String?,
) {

    fun getBurstId(): String? {
        if (name.isNullOrEmpty() || path?.contains("/DCIM/") != true) {
            return null
        }
        return burstPatterns.firstNotNullOfOrNull {
            it.find(name)?.groupValues?.getOrNull(1)
        }
    }

    private companion object {

        /**
         * Google Pixel: PXL_YYYYMMDD_HHMMSS.burst:UUID_COVER.jpg
         * Samsung: 20260909_101500_Burst01.jpg
         * Generic/Legacy: IMG_YYYYMMDD_HHMMSS_BURST1.jpg
         */
        val burstPatterns = arrayOf(
            Regex("_BURST([0-9]+)", RegexOption.IGNORE_CASE),
            Regex(".burst:([-0-9a-f]+)", RegexOption.IGNORE_CASE),
        )
    }
}
