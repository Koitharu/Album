package org.koitharu.album.ui.single

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.theme.AlbumTheme

@AndroidEntryPoint
class SingleViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.dataString
        if (uri.isNullOrEmpty()) {
            finishAfterTransition()
            return
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AlbumTheme(darkTheme = true) {
                SingleViewerScreen(
                    uri = uri,
                    onClose = { finishAfterTransition() },
                )
            }
        }
    }
}