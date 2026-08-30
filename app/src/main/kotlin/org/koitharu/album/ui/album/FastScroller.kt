package org.koitharu.album.ui.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
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
) = BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.TopEnd
) {
    val offsetY = remember { Animatable(0f) }
    val visibility = remember { Animatable(0f) }
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dateTime by remember { mutableStateOf<ImmutableDateTime?>(null) }
    val scope = rememberCoroutineScope()
    var thumbHeightPx by remember { mutableFloatStateOf(0f) }
    val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() } - thumbHeightPx
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(gridState, isDragging) {
        if (!isDragging) {
            snapshotFlow {
                val total = gridState.layoutInfo.totalItemsCount
                if (total == 0) {
                    0f
                } else {
                    gridState.firstVisibleItemIndex / total.toFloat()
                }
            }.collectLatest {
                offsetY.animateTo(it * maxHeightPx)
            }
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.isScrollInProgress || isDragging
        }.transformLatest {
            if (it) {
                emit(true)
            } else {
                delay(3.seconds)
                emit(false)
            }
        }.collectLatest { visible ->
            visibility.animateTo(
                if (visible) 1f else 0f
            )
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow {
            isDragging
        }.transformLatest {
            if (it) {
                emit(true)
            } else {
                delay(1.seconds)
                emit(false)
            }
        }.collectLatest {
            isExpanded = it
        }
    }

    Thumb(
        modifier = Modifier
            .graphicsLayer {
                thumbHeightPx = size.height
                translationY = offsetY.value
                alpha = visibility.value
                translationX = size.width * (1f - visibility.value)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scrollJob?.cancel()
                        scrollJob = scope.launch {
                            val targetOffset = (offsetY.value + dragAmount).coerceIn(
                                minimumValue = 0f,
                                maximumValue = maxHeightPx,
                            )
                            offsetY.snapTo(targetOffset)
                            val fraction = targetOffset / maxHeightPx
                            val targetItem =
                                ((gridState.layoutInfo.totalItemsCount - 1) * fraction).fastRoundToInt()
                            dateTime = dateProvider(targetItem)
                            gridState.scrollToItem(targetItem)
                        }
                    }
                )
            },
        title = if (isExpanded) dateTime?.format("LLLL")?.toTitleCase() else null,
        subtitle = if (isExpanded) dateTime?.format("yyyy") else null,
    )
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
        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    verticalAlignment = Alignment.CenterVertically,
) {
    AnimatedVisibility(
        visible = !title.isNullOrEmpty()
    ) {
        Column(
            modifier = Modifier.padding(
                start = 8.dp,
                end = 32.dp,
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