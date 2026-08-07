package org.koitharu.album.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal typealias SuspendLazyInitializer<T> = suspend () -> T

interface SuspendLazy<T> {

    val isInitialized: Boolean

    suspend fun get(): T

    fun peek(): T?
}

suspend fun <T> SuspendLazy<T>.getOrNull(): T? = runCatchingCancellable {
    get()
}.getOrNull()

suspend fun <R, T : R> SuspendLazy<T>.getOrDefault(defaultValue: R): R = runCatchingCancellable {
    get()
}.getOrDefault(defaultValue)

fun <T> suspendLazy(
    context: CoroutineContext = EmptyCoroutineContext,
    initializer: SuspendLazyInitializer<T>,
): SuspendLazy<T> = SuspendLazyImpl(context, initializer)

private class SuspendLazyImpl<T>(
    private val coroutineContext: CoroutineContext,
    private val initializer: SuspendLazyInitializer<T>,
) : SuspendLazy<T> {

    private val mutex: Mutex = Mutex()
    private var cachedValue: Any? = Uninitialized

    override val isInitialized: Boolean
        get() = cachedValue !== Uninitialized

    @Suppress("UNCHECKED_CAST")
    override suspend fun get(): T {
        // fast way
        cachedValue.let {
            if (it !== Uninitialized) {
                return it as T
            }
        }
        return mutex.withLock {
            cachedValue.let {
                if (it !== Uninitialized) {
                    return it as T
                }
            }
            val result = withContext(coroutineContext) {
                initializer()
            }
            cachedValue = result
            result
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun peek(): T? {
        return cachedValue?.takeUnless { it === Uninitialized } as T?
    }

    private object Uninitialized
}
