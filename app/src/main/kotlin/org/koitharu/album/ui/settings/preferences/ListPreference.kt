package org.koitharu.album.ui.settings.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.ui.theme.AlbumTheme

@Composable
fun <T> ListPreference(
    title: String,
    summary: String? = null,
    entries: ImmutableList<String>,
    entryValues: ImmutableList<T>,
    selectedValue: T?,
    onEntryClick: (T) -> Unit,
) = DialogPreference(
    title = title,
    summary = summary ?: remember(entries, entryValues, selectedValue) {
        entries.getOrNull(entryValues.indexOf(selectedValue))
    },
) { onDismissRequest ->
    DialogContent(
        entries = entries,
        entryValues = entryValues,
        onEntryClick = onEntryClick,
        selectedValue = selectedValue,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun <T> DialogContent(
    entries: ImmutableList<String>,
    entryValues: ImmutableList<T>,
    selectedValue: T?,
    onEntryClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
) = LazyColumn {
    items(entries.size) { i ->
        val value = entryValues[i]
        val isSelected = selectedValue == value
        Surface(
            modifier = Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize(),
            onClick = {
                onEntryClick(value)
                onDismissRequest()
            },
        ) {
            Row(
                modifier = Modifier.padding(
                    vertical = 8.dp,
                    horizontal = 12.dp,
                ).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                )
                Text(
                    text = entries[i],
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewListPreference() = AlbumTheme {
    DialogContent<String>(
        entries = persistentListOf("Item 1", "Item 2"),
        entryValues = persistentListOf("1", "2"),
        onEntryClick = { /* no-op */ },
        selectedValue = "2",
        onDismissRequest = { /* no-op */ })
}