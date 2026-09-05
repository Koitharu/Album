package org.koitharu.album.ui.viewer

import android.media.MediaPlayer
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import java.lang.ref.WeakReference

class VolumeController private constructor(
    private val delegate: MutableFloatState,
) : FloatState by delegate {

    constructor(isMuted: Boolean) : this(mutableFloatStateOf(if (isMuted) 0f else 1f))

    private var mediaPlayer: WeakReference<MediaPlayer>? = null

    fun setMediaPlayer(mp: MediaPlayer?) {
        mediaPlayer = mp?.let { WeakReference(it) }
        mp?.setVolume(floatValue, floatValue)
    }

    fun muteOrUnmute() = setValueInternal(
        if (delegate.floatValue <= MUTE_THRESHOLD) {
            1f
        } else {
            0f
        }
    )

    private fun setValueInternal(newValue: Float) {
        delegate.floatValue = newValue
        mediaPlayer?.get()?.setVolume(newValue, newValue)
    }

    companion object {

        const val MUTE_THRESHOLD = 0.01f
    }
}