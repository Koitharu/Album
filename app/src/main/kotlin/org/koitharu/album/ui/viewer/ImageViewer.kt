package org.koitharu.album.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumItem

@Composable
fun ImageViewer(
    modifier: Modifier,
    image: AlbumItem.Image,
    innerPadding: PaddingValues,
    onClick: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize(),
) {
    var error by remember { mutableStateOf<Throwable?>(null) }
    ZoomableAsyncImage(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        model = ImageRequest.Builder(LocalContext.current).data(image.uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .placeholderMemoryCacheKey(image.memoryCacheKey)
            .listener(
                onError = { _, result -> error = result.throwable },
                onSuccess = { _, _ -> error = null }
            )
            .build(),
        contentPadding = innerPadding,
        contentDescription = null,
        onClick = { onClick() },
    )
    AnimatedVisibility(
        modifier = Modifier
            .padding(24.dp)
            .align(Alignment.Center),
        visible = error != null,
    ) {
        Text(
            text = error?.message ?: stringResource(R.string.error_message_generic),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}