package org.koitharu.album.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlin.time.Duration

fun tickerFlow(delay: Duration): Flow<Long> = channelFlow {
    while (isActive && !trySend(System.currentTimeMillis()).isClosed) {
        delay(delay)
    }
}

@Composable
@NonRestartableComposable
fun <T> FlowCollectEffect(flow: Flow<T>, collector: FlowCollector<T>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner) {
        flow
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
            .collect(collector)
    }
}