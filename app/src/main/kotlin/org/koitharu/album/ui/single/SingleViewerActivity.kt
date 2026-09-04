package org.koitharu.album.ui.single

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.theme.resolveThemeVariant

@AndroidEntryPoint
class SingleViewerActivity : ComposeActivity() {

    @Composable
    override fun Content() {
        val uri = intent.dataString
        LaunchedEffect(uri.isNullOrEmpty()) {
            if (uri.isNullOrEmpty()) {
                finishAfterTransition()
            }
        }
        if (uri.isNullOrEmpty()) {
            return
        }
        AlbumTheme(variant = resolveThemeVariant(isForViewer = true)) {
            SingleViewerScreen(
                uri = uri,
                onClose = { finishAfterTransition() },
            )
            SetSystemBarsColorsEffect()
        }
    }
}