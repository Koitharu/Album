package org.koitharu.album.util

import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> runCancellable(block: (CancellationSignal) -> T): T {
    return suspendCancellableCoroutine<T> { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        val result = block(signal)
        cont.resume(result)
    }
}