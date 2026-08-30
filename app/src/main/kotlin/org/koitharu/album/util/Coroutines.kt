package org.koitharu.album.util

import android.os.CancellationSignal
import android.os.OperationCanceledException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> runCancellable(block: (CancellationSignal) -> T): T {
    return suspendCancellableCoroutine<T> { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        try {
            val result = block(signal)
            cont.resume(result)
        } catch (e: OperationCanceledException) {
            cont.cancel(e)
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
    }
}