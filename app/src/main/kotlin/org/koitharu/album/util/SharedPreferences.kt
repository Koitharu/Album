package org.koitharu.album.util

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun SharedPreferences.observeChanges(): Flow<String?> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        trySendBlocking(key)
    }
    registerOnSharedPreferenceChangeListener(listener)
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}