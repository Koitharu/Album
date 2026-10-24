package org.koitharu.album.ui.single

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.common.ShellIntegrationHelper
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.theme.resolveThemeVariant
import javax.inject.Inject

@AndroidEntryPoint
class SingleViewerActivity : ComposeActivity() {

    @Inject
    lateinit var shellIntegrationHelper: ShellIntegrationHelper

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
                onShareClick = { shareImage(uri) },
            )
            SetSystemBarsColorsEffect()
        }
    }

    private fun shareImage(uri: String) {
        lifecycleScope.launch {
            shellIntegrationHelper.shareImage(uri)
        }
    }
}