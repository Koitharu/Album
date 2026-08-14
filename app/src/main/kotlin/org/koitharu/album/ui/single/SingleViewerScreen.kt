package org.koitharu.album.ui.single

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koitharu.album.R
import org.koitharu.album.util.rememberWindowInsetsController
import org.koitharu.album.util.shareImage
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun SingleViewerScreen(
    uri: String,
    onClose: () -> Unit,
) {
    var isUiVisible by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TopAppBar(
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        IconButtonWithTooltip(
                            tooltip = stringResource(R.string.back),
                            onClick = onClose,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BottomBar(uri)
            }
        },
        contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
    ) { innerPadding ->
        val insetsController = rememberWindowInsetsController()
        if (insetsController != null) {
            DisposableEffect(Unit) {
                onDispose {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            LaunchedEffect(isUiVisible) {
                if (isUiVisible) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            var error by remember { mutableStateOf<Throwable?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            ZoomableAsyncImage(
                modifier = Modifier
                    .fillMaxSize(),
                model = ImageRequest.Builder(LocalContext.current).data(uri)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .listener(
                        onError = { _, result -> error = result.throwable; isLoading = false },
                        onSuccess = { _, _ -> error = null; isLoading = false },
                    )
                    .build(),
                contentDescription = null,
                onClick = { isUiVisible = !isUiVisible },
            )
            AnimatedVisibility(
                modifier = Modifier
                    .padding(innerPadding + PaddingValues(24.dp))
                    .align(Alignment.Center),
                visible = error != null,
            ) {
                Text(
                    text = error?.message ?: stringResource(R.string.error_message_generic),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            AnimatedVisibility(
                modifier = Modifier
                    .padding(innerPadding)
                    .align(Alignment.Center),
                visible = isLoading,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
private fun BottomBar(
    uri: String,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            vertical = 12.dp,
        ),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
) {
    val context = LocalContext.current
    IconButtonWithTooltip(
        tooltip = stringResource(R.string.share),
        onClick = { shareImage(context, uri) },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.share)
        )
    }
}