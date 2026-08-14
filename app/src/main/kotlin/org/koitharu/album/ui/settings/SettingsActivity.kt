package org.koitharu.album.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.theme.AlbumTheme

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.collectState()
            AlbumTheme {
                SettingsScreen(
                    state = state,
                    handleIntent = viewModel,
                    onClose = { finishAfterTransition() },
                )
            }
        }
    }
}