package org.koitharu.album.ui.picker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.picker.PickerEffect.OnError
import org.koitharu.album.ui.picker.PickerEffect.ReturnMultipleItems
import org.koitharu.album.ui.picker.PickerEffect.ReturnSingleItem
import org.koitharu.album.util.FlowCollectEffect
import org.koitharu.album.util.rememberPermissionCheck
import org.koitharu.album.util.rememberPermissionsCheck

@AndroidEntryPoint
class PickerActivity : ComposeActivity() {

    @Composable
    override fun Content() {
        val isPermissionGranted by
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionsCheck(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            rememberPermissionCheck(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        if (!isPermissionGranted) {
            EmptyState(
                modifier = Modifier.fillMaxSize(),
                iconResId = R.drawable.ic_folder_alert,
                title = stringResource(R.string.no_permissions),
                message = stringResource(R.string.no_permissions_message),
            )
            return
        }
        val options = remember(intent) {
            PickerOptions.from(intent)
        }
        val viewModel = hiltViewModel<PickerViewModel, PickerViewModel.Factory> {
            it.create(options)
        }
        val state by viewModel.collectState()
        val snackbarHostState = remember { SnackbarHostState() }
        FlowCollectEffect(viewModel.effect) { effect ->
            when (effect) {
                is OnError -> snackbarHostState.showSnackbar(
                    effect.error.message ?: getString(R.string.error_message_generic)
                )

                is ReturnMultipleItems -> returnMultipleResults(effect.items.map { it.uri })
                is ReturnSingleItem -> returnSingleResult(effect.item.uri)
            }
        }
        PickerContent(
            state = state,
            content = viewModel.content,
            snackbarHostState = snackbarHostState,
            handleIntent = viewModel,
            options = options,
            onClose = { cancelSelection() },
        )
    }

    private fun returnSingleResult(fileUri: Uri) {
        val resultIntent = Intent().apply {
            data = fileUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        setResult(RESULT_OK, resultIntent)
        finishAfterTransition()
    }

    private fun returnMultipleResults(fileUris: List<Uri>) {
        val resultIntent = Intent().apply {
            val clipData = android.content.ClipData.newRawUri("Selected Media", fileUris.first())
            for (i in 1 until fileUris.size) {
                clipData.addItem(android.content.ClipData.Item(fileUris[i]))
            }
            setClipData(clipData)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        setResult(RESULT_OK, resultIntent)
        finishAfterTransition()
    }

    private fun cancelSelection() {
        setResult(RESULT_CANCELED)
        finishAfterTransition()
    }
}