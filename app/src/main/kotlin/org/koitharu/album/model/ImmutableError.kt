package org.koitharu.album.model

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalResources
import org.koitharu.album.R

@Immutable
class ImmutableError(
    error: Throwable,
) {

    @Stable
    val message = error.message

    fun getUserFriendlyMessage(resources: Resources) =
        message ?: resources.getString(R.string.error_message_generic)
}

@Composable
@ReadOnlyComposable
fun ImmutableError.userFriendlyMessage() = getUserFriendlyMessage(LocalResources.current)