package org.koitharu.album.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.home.HomeNavHost
import org.koitharu.album.ui.settings.SettingsActivity
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.rememberPermissionCheck
import org.koitharu.album.util.rememberPermissionsCheck

@AndroidEntryPoint
class MainActivity : ComposeActivity() {

    @Composable
    override fun Content() {
        AlbumTheme {
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
            if (isPermissionGranted) {
                HomeNavHost()
            } else {
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
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun OptionsMenuContent(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(R.string.settings)) },
        onClick = {
            onDismissRequest()
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    )
}