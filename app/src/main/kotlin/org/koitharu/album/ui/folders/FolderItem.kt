package org.koitharu.album.ui.folders

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koitharu.album.R

@Immutable
sealed interface FolderItem : Parcelable {

    val id: String
    val size: Int
    val thumbnail: Uri?

    @get:DrawableRes
    val iconId: Int

    @Composable
    @ReadOnlyComposable
    fun title() = when (this) {
        is Bucket -> name
        is Favorites -> stringResource(R.string.favorites)
        is RecycleBin -> stringResource(R.string.recycle_bin)
        is Photos -> stringResource(R.string.photos)
    }

    @Parcelize
    data class Favorites(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_favorites"

        override val iconId: Int
            get() = R.drawable.ic_star_outline
    }

    @Parcelize
    data class RecycleBin(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_trash"

        override val iconId: Int
            get() = R.drawable.ic_delete
    }

    @Parcelize
    data class Photos(
        override val size: Int,
        override val thumbnail: Uri?
    ) : FolderItem {

        @IgnoredOnParcel
        override val id = "_dcim"

        override val iconId: Int
            get() = R.drawable.ic_photo_camera
    }

    @Parcelize
    data class Bucket(
        override val id: String,
        val name: String,
        override val size: Int,
        override val thumbnail: Uri,
    ) : FolderItem {

        override val iconId: Int
            get() = R.drawable.ic_folders
    }
}
