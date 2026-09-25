package org.koitharu.album.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.koitharu.album.R
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun SearchBar(
    modifier: Modifier,
    state: SearchState,
    onCollapse: () -> Unit,
) = Box(
    modifier = modifier,
) {
    val focusRequester = remember { FocusRequester() }
    SearchBarDefaults.InputField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textFieldState = state.textFieldState,
        searchBarState = state.searchBarState,
        onSearch = { },
        placeholder = {
            Text(
                modifier = Modifier.clearAndSetSemantics {},
                text = stringResource(R.string.search),
            )
        },
        leadingIcon = {
            IconButtonWithTooltip(
                onClick = onCollapse,
                tooltip = stringResource(R.string.close),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        trailingIcon = {
            IconButtonWithTooltip(
                onClick = { state.clearText() },
                tooltip = stringResource(R.string.clear),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.clear),
                )
            }
        },
    )
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    )
    LaunchedEffect(Unit) {
        if (state.textFieldState.text.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}