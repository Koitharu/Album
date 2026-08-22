package org.koitharu.album.ui.album

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
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
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Hide
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Recover
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Share
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Trash
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Unfavorite
import org.koitharu.album.ui.album.AlbumIntent.SelectionAlbumIntent.Unhide
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun AlbumActionMode(
    selectedItemCount: Int,
    folder: FolderItem?,
    isRecycleBinEnabled: Boolean,
    onCancel: () -> Unit,
    handleIntent: MviIntentHandler<SelectionAlbumIntent>,
) = HorizontalFloatingToolbar(
    expanded = true,
    leadingContent = {
        IconButtonWithTooltip(
            tooltip = stringResource(android.R.string.cancel),
            tooltipAnchorPosition = TooltipAnchorPosition.Above,
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
            tooltipAnchorPosition = TooltipAnchorPosition.Above,
            onClick = { handleIntent(Share) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.share),
            )
        }
        if (folder is FolderItem.Favorites) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.remove_from_favorites),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Unfavorite) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_off),
                    contentDescription = stringResource(R.string.remove_from_favorites),
                )
            }
        }
        if (folder is FolderItem.Hidden) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.unhide),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Unhide) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_show),
                    contentDescription = stringResource(R.string.unhide),
                )
            }
        } else if (folder == null) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.hide),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Hide) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_hide),
                    contentDescription = stringResource(R.string.hide),
                )
            }
        }
        if (folder is FolderItem.RecycleBin) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.restore),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Recover) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recover),
                    contentDescription = stringResource(R.string.restore),
                )
            }
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.delete_permanently),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Delete) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_forever),
                    contentDescription = stringResource(R.string.delete_permanently),
                )
            }
        } else if (isRecycleBinEnabled) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.move_to_recycle_bin),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Trash) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.move_to_recycle_bin),
                )
            }
        } else {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.delete),
                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                onClick = { handleIntent(Delete) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_forever),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
)

@Preview
@Composable
private fun PreviewAlbumActionModeLight() = AlbumTheme(
    variant = ThemeVariant.LIGHT,
) {
    AlbumActionMode(
        folder = null,
        selectedItemCount = 5,
        isRecycleBinEnabled = true,
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
        folder = null,
        selectedItemCount = 5,
        isRecycleBinEnabled = false,
        onCancel = { /* no-op */ },
        handleIntent = MviIntentHandler.NoOp,
    )
}