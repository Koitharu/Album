package org.koitharu.album.ui.editor

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
import org.koitharu.album.ui.editor.ImageEditorEffect.OnError
import org.koitharu.album.ui.editor.ImageEditorEffect.OnImageSaved
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.FlowCollectEffect

@AndroidEntryPoint
class ImageEditorActivity : ComposeActivity() {

    @Composable
    override fun Content() {
        val uri = remember { intent.dataString }
        LaunchedEffect(uri.isNullOrEmpty()) {
            if (uri.isNullOrEmpty()) {
                finishAfterTransition()
            }
        }
        if (uri.isNullOrEmpty()) {
            return
        }
        val name = remember {
            intent.getStringExtra(EXTRA_NAME)
        }
        AlbumTheme {
            val viewModel = hiltViewModel<ImageEditorViewModel, ImageEditorViewModel.Factory> {
                it.create(uri, name)
            }
            val snackbarHostState = remember { SnackbarHostState() }
            FlowCollectEffect(viewModel.effect) { effect ->
                when (effect) {
                    is OnError -> snackbarHostState.showSnackbar(
                        effect.error.message ?: getString(R.string.error_message_generic)
                    )

                    OnImageSaved -> {
                        Toast.makeText(this, R.string.image_saved_successfully, Toast.LENGTH_LONG)
                            .show()
                        finishAfterTransition()
                    }
                }
            }
            val state by viewModel.collectState()
            ImageEditorScreen(
                snackbarHostState = snackbarHostState,
                state = state,
                handleIntent = viewModel,
                onClose = { finishAfterTransition() },
            )
        }
    }

    companion object {

        const val EXTRA_NAME = "org.koitharu.album.ui.editor.IMAGE_NAME"
    }
}