package org.koitharu.album.ui.folders

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koitharu.album.R
import org.koitharu.album.model.MediaQuery

@Immutable
sealed interface FolderItem : Parcelable {

    val id: String
    val size: Int
    val thumbnail: Uri?

    @Stable
    @get:DrawableRes
    val iconId: Int

    @Composable
    @ReadOnlyComposable
    fun title() = when (this) {
        is Bucket -> name
        is Favorites -> stringResource(R.string.favorites)
        is RecycleBin -> stringResource(R.string.recycle_bin)
        is Photos -> stringResource(R.string.photos)
        is Videos -> stringResource(R.string.videos)
        is Hidden -> stringResource(R.string.hidden)
    }

    fun getMediaQuery(): MediaQuery

    @Parcelize
    @Immutable
    data class Favorites(
        override val size: Int,
        override val thumbnail: Uri?,
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_favorites"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_star_outline

        override fun getMediaQuery() = MediaQuery(
            isFavorites = true,
        )
    }

    @Parcelize
    @Immutable
    data class RecycleBin(
        override val size: Int,
        override val thumbnail: Uri?,
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_trash"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_delete

        override fun getMediaQuery() = MediaQuery(
            isTrashed = true,
        )
    }

    @Parcelize
    @Immutable
    data class Photos(
        override val size: Int,
        override val thumbnail: Uri?,
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_dcim"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_photo_camera

        override fun getMediaQuery() = MediaQuery(
            includeVideos = false,
            pathPattern = "DCIM",
        )
    }

    @Parcelize
    @Immutable
    data class Videos(
        override val size: Int,
        override val thumbnail: Uri?,
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_videos"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_movie

        override fun getMediaQuery() = MediaQuery(
            includeImages = false,
        )
    }

    @Parcelize
    @Immutable
    data class Hidden(
        override val size: Int,
        override val thumbnail: Uri?,
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_hidden"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_hide

        override fun getMediaQuery() = MediaQuery(
            isHidden = true,
        )
    }

    @Parcelize
    @Immutable
    data class Bucket(
        override val id: String,
        val name: String,
        override val size: Int,
        override val thumbnail: Uri,
    ) : FolderItem {

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_folder

        override fun getMediaQuery() = MediaQuery(
            bucketIds = persistentSetOf(id),
        )
    }
}
