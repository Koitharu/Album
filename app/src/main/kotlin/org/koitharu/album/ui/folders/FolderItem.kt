package org.koitharu.album.ui.folders

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koitharu.album.R

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

    @Parcelize
    @Immutable
    data class Favorites(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_favorites"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_star_outline
    }

    @Parcelize
    @Immutable
    data class RecycleBin(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_trash"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_delete
    }

    @Parcelize
    @Immutable
    data class Photos(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_dcim"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_photo_camera
    }

    @Parcelize
    @Immutable
    data class Videos(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_videos"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_movie
    }

    @Parcelize
    @Immutable
    data class Hidden(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_hidden"

        @Stable
        override val iconId: Int
            get() = R.drawable.ic_hide
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
            get() = R.drawable.ic_folders
    }
}
