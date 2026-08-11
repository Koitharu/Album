package org.koitharu.album.ui.album

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koitharu.album.R
import org.koitharu.album.model.ImmutableDateTime
import org.koitharu.album.model.format
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.toTitleCase
import kotlin.time.Duration.Companion.seconds

@Composable
fun FastScroller(
    modifier: Modifier,
    gridState: LazyGridState,
    dateProvider: (Int) -> ImmutableDateTime?,
) {
    val coroutineScope = rememberCoroutineScope()
    var maxHeightPx by remember { mutableIntStateOf(0) }
    var thumbHeightPx by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dateTime by remember { mutableStateOf<ImmutableDateTime?>(null) }

    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(2.seconds)
            isVisible = false
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 0f
            val firstVisibleIndex = gridState.firstVisibleItemIndex
            firstVisibleIndex.toFloat() / totalItems.toFloat()
        }
    }

    val availableHeightPx = remember(maxHeightPx, thumbHeightPx) {
        (maxHeightPx - thumbHeightPx).coerceAtLeast(0)
    }

    val thumbOffsetPx by remember(availableHeightPx) {
        derivedStateOf {
            (scrollFraction * availableHeightPx).toInt().coerceIn(0, availableHeightPx)
        }
    }

    var scrollJob by remember { mutableStateOf<Job?>(null) }
    val draggableState = rememberDraggableState { delta ->
        val totalItems = gridState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetOffset =
                (thumbOffsetPx + delta).toInt().coerceIn(0, availableHeightPx)
            val targetFraction = if (availableHeightPx > 0) {
                targetOffset / availableHeightPx.toFloat()
            } else {
                0f
            }
            val targetIndex = (totalItems * targetFraction).toInt()
                .coerceIn(0, totalItems - 1)
            dateTime = dateProvider(targetIndex)
            val prevJob = scrollJob
            scrollJob = coroutineScope.launch {
                prevJob?.cancelAndJoin()
                Log.i("FASTSCROLL", "Scroll to $targetIndex")
                gridState.scrollToItem(targetIndex)
            }
        }
    }

    AnimatedVisibility(
        modifier = Modifier
            .fillMaxHeight()
            .then(modifier),
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth }
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .onGloballyPositioned {
                    maxHeightPx = it.size.height
                }
                .draggable(
                    state = draggableState,
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false },
                    orientation = Orientation.Vertical
                )
        ) {
            Thumb(
                modifier = Modifier
                    .onGloballyPositioned {
                        thumbHeightPx = it.size.height
                    }
                    .offset { IntOffset(0, thumbOffsetPx) },
                title = if (isDragging) dateTime?.format("LLLL")?.toTitleCase() else null,
                subtitle = if (isDragging) dateTime?.format("yyyy") else null,
            )
        }
    }
}

@Composable
private fun Thumb(
    modifier: Modifier,
    title: String?,
    subtitle: String?,
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
    AnimatedVisibility(
        visible = !title.isNullOrEmpty()
    ) {
        Column(
            modifier = Modifier.padding(
                start = 8.dp,
            ),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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
        title = null,
        subtitle = null,
    )
}

@Preview(showBackground = true)
@Composable
private fun ThumbPreviewWithText() = AlbumTheme {
    Thumb(
        modifier = Modifier.padding(42.dp),
        title = "March",
        subtitle = "2007",
    )
}