package org.koitharu.album.ui.settings.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun DialogPreference(
    title: String,
    summary: String? = null,
    dialogContent: @Composable (onDismissRequest: () -> Unit) -> Unit,
) {
    var isDialogVisible by rememberSaveable() { mutableStateOf(false) }
    Preference(
        title = title,
        summary = summary,
        onClick = { isDialogVisible = true },
    )
    if (isDialogVisible) {
        PreferenceDialog(
            onDismissRequest = { isDialogVisible = false },
            dialogContent = dialogContent,
        )
    }
}

@Composable
private fun PreferenceDialog(
    onDismissRequest: () -> Unit,
    dialogContent: @Composable (onDismissRequest: () -> Unit) -> Unit,
) = BasicAlertDialog(
    onDismissRequest = onDismissRequest
) {
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            dialogContent(onDismissRequest)
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}