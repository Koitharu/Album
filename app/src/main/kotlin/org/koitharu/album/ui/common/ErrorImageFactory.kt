package org.koitharu.album.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import coil3.Image
import coil3.asImage
import coil3.request.ImageRequest
import org.koitharu.album.R

class ErrorImageFactory private constructor(
    private val theme: MaterialTheme.Values
) : (ImageRequest) -> Image? {

    override fun invoke(request: ImageRequest): Image? {
        val drawable = ContextCompat.getDrawable(
            request.context,
            R.drawable.ic_broken_image,
        )?.mutate() ?: return null
        drawable.setTint(theme.colorScheme.surfaceTint.toArgb())
        return drawable.asImage()
    }

    companion object {

        @Composable
        operator fun invoke() = ErrorImageFactory(
            LocalMaterialTheme.current
        )
    }
}