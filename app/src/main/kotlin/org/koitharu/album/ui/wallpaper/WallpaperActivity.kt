package org.koitharu.album.ui.wallpaper

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.wallpaper.WallpaperEffect.OnError
import org.koitharu.album.ui.wallpaper.WallpaperEffect.OnWallpaperApplied
import org.koitharu.album.util.FlowCollectEffect

@AndroidEntryPoint
class WallpaperActivity : ComposeActivity() {

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
        AlbumTheme {
            val viewModel = hiltViewModel<WallpaperViewModel, WallpaperViewModel.Factory> {
                it.create(uri)
            }
            val snackbarHostState = remember { SnackbarHostState() }
            FlowCollectEffect(viewModel.effect) { effect ->
                when (effect) {
                    is OnError -> snackbarHostState.showSnackbar(
                        effect.error.message ?: getString(R.string.error_message_generic)
                    )

                    OnWallpaperApplied -> {
                        Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_LONG).show()
                        moveTaskToBack(true)
                        finishAfterTransition()
                    }
                }
            }
            val state by viewModel.collectState()
            WallpaperScreen(
                state = state,
                handleIntent = viewModel,
                snackbarHostState = snackbarHostState,
                onBack = { finishAfterTransition() },
            )
        }
    }
}