package org.koitharu.album.ui.album

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koitharu.album.R
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Delete
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Share
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun AlbumActionMode(
    selectedItemCount: Int,
    onCancel: () -> Unit,
    handleIntent: MviIntentHandler<SelectionAlbumIntent>,
) = HorizontalFloatingToolbar(
    expanded = true,
    leadingContent = {
        IconButtonWithTooltip(
            tooltip = stringResource(android.R.string.cancel),
            onClick = onCancel,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(android.R.string.cancel),
            )
        }
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = selectedItemCount.toString(),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
        )
        Spacer(
            modifier = Modifier.width(12.dp)
        )
    },
    content = {
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.share),
            onClick = { handleIntent(Share) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.share),
            )
        }
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.delete),
            onClick = { handleIntent(Delete) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
            )
        }
    }
)

@Preview
@Composable
private fun PreviewAlbumActionModeLight() = AlbumTheme(
    variant = ThemeVariant.LIGHT,
) {
    AlbumActionMode(
        selectedItemCount = 5,
        onCancel = { /* no-op */ },
        handleIntent = MviIntentHandler.NoOp,
    )
}

@Preview
@Composable
private fun PreviewAlbumActionModeDark() = AlbumTheme(
    variant = ThemeVariant.DARK,
) {
    AlbumActionMode(
        selectedItemCount = 5,
        onCancel = { /* no-op */ },
        handleIntent = MviIntentHandler.NoOp,
    )
}