package org.koitharu.album.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface IntentSenderLauncher {

    suspend fun awaitIntentSenderForResult(intentSender: IntentSender): Intent?

    companion object {

        tailrec fun from(context: Context): IntentSenderLauncher? = when (context) {
            is IntentSenderLauncher -> context
            is ContextWrapper -> from(context.baseContext)
            else -> null
        }
    }
}

class IntentSenderLauncherRegistry(
    caller: ActivityResultCaller,
) : IntentSenderLauncher {
    private val intentSenderLauncher = caller.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = if (result.resultCode == Activity.RESULT_OK) {
            result.data ?: Intent()
        } else {
            null
        }
        continuation?.resume(data)
    }
    private var continuation: Continuation<Intent?>? = null
    private val mutex = Mutex()

    @Suppress("SuspendCoroutineLacksCancellationGuarantees")
    override suspend fun awaitIntentSenderForResult(
        intentSender: IntentSender
    ): Intent? = mutex.withLock {
        check(continuation == null)
        try {
            suspendCoroutine { cont ->
                continuation = cont
                val request = IntentSenderRequest.Builder(
                    intentSender = intentSender,
                ).build()
                intentSenderLauncher.launch(request)
            }
        } finally {
            continuation = null
        }
    }
}