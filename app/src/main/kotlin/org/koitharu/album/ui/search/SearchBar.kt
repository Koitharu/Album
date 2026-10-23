package org.koitharu.album.ui.search

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import org.koitharu.album.R
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun SharedTransitionScope.SearchBar(
    modifier: Modifier,
    text: String,
    onTextChanged: (String) -> Unit,
    onCollapse: () -> Unit,
) = Box(
    modifier = modifier,
) {
    val focusRequester = remember { FocusRequester() }
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .sharedElement(
                sharedContentState = rememberSharedContentState("search"),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current
            )
            .focusRequester(focusRequester),
        value = text,
        onValueChange = onTextChanged,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
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
                onClick = { onTextChanged("") },
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
        if (text.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}