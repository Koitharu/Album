package org.koitharu.album.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.koitharu.album.R
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun OptionsMenu(
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current,
    content: @Composable ColumnScope.(onDismissRequest: () -> Unit) -> Unit,
) = Box(modifier = modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButtonWithTooltip(
        onClick = { isExpanded = true },
        tooltip = stringResource(R.string.menu),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu),
            contentDescription = stringResource(R.string.menu),
            tint = iconColor,
        )
    }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
        content = {
            content { isExpanded = false }
        },
    )
}