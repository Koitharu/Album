package org.koitharu.album.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.koitharu.album.R
import org.koitharu.album.model.ImmutableError
import org.koitharu.album.model.userFriendlyMessage

@Composable
fun ErrorDialog(
    error: ImmutableError,
    onDismissRequest: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
        Text(
            text = stringResource(R.string.error)
        )
    },
    text = {
        Text(
            text = error.userFriendlyMessage()
        )
    },
    confirmButton = {
        TextButton(onClick = {
            onDismissRequest()
        }) { Text(stringResource(android.R.string.ok)) }
    },
)