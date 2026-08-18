package org.koitharu.album.ui.common

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import org.koitharu.album.util.IntentSenderLauncher
import org.koitharu.album.util.IntentSenderLauncherRegistry

abstract class ComposeActivity : ComponentActivity(), IntentSenderLauncher {

    private val intentSenderLauncherRegistry = IntentSenderLauncherRegistry(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Content()
        }
    }

    override suspend fun awaitIntentSenderForResult(
        intentSender: IntentSender
    ): Intent? = intentSenderLauncherRegistry.awaitIntentSenderForResult(intentSender)

    @Composable
    abstract fun Content()
}