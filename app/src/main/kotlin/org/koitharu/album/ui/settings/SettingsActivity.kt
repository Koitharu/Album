package org.koitharu.album.ui.settings

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.theme.AlbumTheme

@AndroidEntryPoint
class SettingsActivity : ComposeActivity() {

    private val viewModel by viewModels<SettingsViewModel>()

    @Composable
    override fun Content() {
        AlbumTheme {
            val state by viewModel.collectState()
            SettingsScreen(
                state = state,
                handleIntent = viewModel,
                onClose = { finishAfterTransition() },
            )
        }
    }
}