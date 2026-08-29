package org.koitharu.album.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import kotlinx.collections.immutable.ImmutableList
import org.koitharu.album.R

@Composable
fun <T> SpinnerButton(
    items: ImmutableList<T>,
    selectedItem: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltip: String,
    tooltipAnchorPosition: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    onItemClick: (T) -> Unit,
    content: @Composable (T, Boolean) -> Unit,
) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(tooltipAnchorPosition),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = tooltipState,
    ) {
        Box(modifier = modifier) {
            var isExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .clickable(
                        enabled = enabled,
                        onClick = { isExpanded = !isExpanded },
                        role = Role.DropdownList,
                    )
                    .minimumInteractiveComponentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content(selectedItem, true)
                Icon(
                    modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
                    painter = painterResource(R.drawable.ic_drop_down),
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelMedium) {
                    items.forEach {
                        DropdownMenuItem(
                            text = {
                                content(it, false)
                            },
                            onClick = {
                                onItemClick(it)
                                isExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}