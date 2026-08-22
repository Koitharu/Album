package org.koitharu.album.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koitharu.album.R
import org.koitharu.album.ui.theme.AlbumTheme

@Composable
fun EmptyState(
    modifier: Modifier,
    @DrawableRes iconResId: Int,
    title: String,
    message: String,
    additionalContent: (@Composable () -> Unit)? = null,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
    Icon(
        modifier = Modifier
            .size(128.dp),
        painter = painterResource(iconResId),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.surfaceDim,
    )
    Text(
        modifier = Modifier
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
            ),
        textAlign = TextAlign.Center,
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        modifier = Modifier
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
            ),
        text = message,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
    additionalContent?.let { content ->
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        content()
    }
}

@Preview
@Composable
private fun PreviewEmptyState() = AlbumTheme {
    EmptyState(
        modifier = Modifier.fillMaxSize(),
        iconResId = R.drawable.ic_folder_alert,
        title = stringResource(R.string.no_permissions),
        message = stringResource(R.string.no_permissions_message),
    ) {
        Button(
            onClick = {/* no-op */ }
        ) {
            Text(
                text = stringResource(R.string.settings)
            )
        }
    }
}