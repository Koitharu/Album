package org.koitharu.album.ui.folders

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import org.koitharu.album.R

@Composable
fun FoldersContent(
    innerPadding: PaddingValues,
    listState: LazyListState,
    onFolderClick: (FolderItem) -> Unit,
) {
    val viewModel = hiltViewModel<FoldersViewModel>()
    val state by viewModel.collectState()
    val folders = state.items
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
        contentPadding = innerPadding,
    ) {
        items(
            items = folders,
            key = { it.id },
        ) {
            Folder(
                folder = it,
                onClick = { onFolderClick(it) }
            )
        }
        state.error?.let { error ->
            item(
                key = "error",
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = error.message ?: stringResource(R.string.error_message_generic),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun Folder(
    folder: FolderItem,
    onClick: () -> Unit,
) = Surface(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val title = folder.title()
        if (folder.thumbnail != null) {
            AsyncImage(
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.medium),
                model = folder.thumbnail,
                contentDescription = title,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.medium
                    ),
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .alpha(0.6f),
                    painter = painterResource(folder.iconId),
                    contentDescription = null,
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = pluralStringResource(R.plurals.items, folder.size, folder.size),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}