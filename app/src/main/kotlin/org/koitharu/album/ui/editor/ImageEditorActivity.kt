package org.koitharu.album.ui.editor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.common.ErrorDialog
import org.koitharu.album.ui.editor.ImageEditorEffect.OnError
import org.koitharu.album.ui.editor.ImageEditorEffect.OnImageSaved
import org.koitharu.album.ui.editor.ImageEditorIntent.ClearError
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.FlowCollectEffect
import org.koitharu.album.util.rememberPermissionCheck

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
        val isPermissionGranted by
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionCheck(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            remember { mutableStateOf(true) }
        }
        if (!isPermissionGranted) {
            EmptyState(
                modifier = Modifier.fillMaxSize(),
                iconResId = R.drawable.ic_folder_alert,
                title = stringResource(R.string.no_permissions),
                message = stringResource(R.string.no_permissions_message),
            ) {
                Button(
                    onClick = { openAppSettings() }
                ) {
                    Text(
                        text = stringResource(R.string.settings)
                    )
                }
            }
            return
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
            state.error?.let { error ->
                ErrorDialog(
                    error = error,
                    onDismissRequest = { viewModel.handleIntent(ClearError) },
                )
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    companion object {

        const val EXTRA_NAME = "org.koitharu.album.ui.editor.IMAGE_NAME"
    }
}