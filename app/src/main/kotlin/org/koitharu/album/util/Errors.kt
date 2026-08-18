package org.koitharu.album.util

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException

inline fun <R> runCatchingCancellable(block: () -> R): Result<R> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

@RequiresApi(Build.VERSION_CODES.Q)
suspend fun RecoverableSecurityException.resolve(context: Context): Boolean {
    val launcher = IntentSenderLauncher.from(context) ?: return false
    return launcher.awaitIntentSenderForResult(userAction.actionIntent.intentSender) != null
}