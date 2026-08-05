package org.koitharu.album.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.koitharu.album.R
import org.koitharu.album.ui.theme.AlbumTheme


@Composable
fun FastScroller(
    modifier: Modifier,
    gridState: LazyGridState,
) = BoxWithConstraints(
    modifier = Modifier
        .fillMaxHeight()
        .then(modifier),
) {
    val coroutineScope = rememberCoroutineScope()
    val maxHeightPx = constraints.maxHeight.toFloat()
    var thumbHeightPx by remember { mutableIntStateOf(0) }

    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 0f
            val firstVisibleIndex = gridState.firstVisibleItemIndex
            firstVisibleIndex.toFloat() / totalItems.toFloat()
        }
    }

    val availableHeightPx = remember(maxHeightPx, thumbHeightPx) {
        maxOf(
            0f,
            maxHeightPx - thumbHeightPx
        )
    }

    val thumbOffsetPx by remember {
        derivedStateOf {
            (scrollProgress * availableHeightPx).coerceIn(0f, availableHeightPx)
        }
    }

    var scrollJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, thumbOffsetPx.toInt()) }
            .pointerInput(availableHeightPx) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val totalItems = gridState.layoutInfo.totalItemsCount
                    if (totalItems > 0) {
                        val newOffset = (thumbOffsetPx + dragAmount).coerceIn(0f, availableHeightPx)

                        val newProgress =
                            if (availableHeightPx > 0) newOffset / availableHeightPx else 0f

                        val targetIndex =
                            (newProgress * totalItems).toInt().coerceIn(0, totalItems - 1)

                        val prevJob = scrollJob
                        scrollJob = coroutineScope.launch {
                            prevJob?.cancelAndJoin()
                            gridState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
    ) {
        Thumb(
            modifier = Modifier.onSizeChanged {
                thumbHeightPx = it.height
            },
            text = null,
        )
    }
}

@Composable
private fun Thumb(
    modifier: Modifier,
    text: String?,
) = Row(
    modifier = modifier
        .shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
            )
        )
        .background(Color.White),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (!text.isNullOrEmpty()) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }

    Icon(
        modifier = Modifier.padding(
            vertical = 10.dp,
            horizontal = 8.dp,
        ),
        painter = painterResource(R.drawable.ic_pan_vertical),
        contentDescription = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun ThumbPreview() = AlbumTheme {
    Thumb(
        modifier = Modifier.padding(42.dp),
        text = null,
    )
}