package org.koitharu.album.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

abstract class MviViewModel<S, I, E>(
    initialState: S,
) : ViewModel(), MviIntentHandler<I> {

    protected val state = MutableStateFlow<S>(initialState)

    val effect: SharedFlow<E>
        field = MutableSharedFlow<E>(extraBufferCapacity = 1)

    protected suspend fun sendEffect(e: E) {
        effect.emit(e)
    }

    protected fun sendEffectAsync(e: E) = viewModelScope.launch {
        sendEffect(e)
    }

    @Composable
    fun collectState() = state.collectAsStateWithLifecycle()
}