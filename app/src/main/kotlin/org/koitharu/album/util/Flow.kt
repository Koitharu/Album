package org.koitharu.album.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlin.time.Duration

public fun tickerFlow(delay: Duration): Flow<Long> = channelFlow {
    while (isActive && !trySend(System.currentTimeMillis()).isClosed) {
        delay(delay)
    }
}