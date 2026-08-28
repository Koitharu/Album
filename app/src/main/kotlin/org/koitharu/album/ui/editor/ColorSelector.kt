package org.koitharu.album.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.R
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun ColorSelector(
    currentColor: Color,
    onChangeColor: (Color) -> Unit,
) = Box {
    val colors = remember {
        persistentListOf(
            Color.Yellow,
            Color.Red,
            Color.Magenta,
            Color.Blue,
            Color.Green,
            Color.Cyan,
            Color.White,
            Color.Gray,
            Color.Black,
        )
    }
    var isExpanded by remember { mutableStateOf(false) }
    val outlineColor = MaterialTheme.colorScheme.outline
    IconButtonWithTooltip(
        tooltip = stringResource(R.string.select_color),
        onClick = { isExpanded = !isExpanded },
    ) {
        Canvas(
            modifier = Modifier.size(24.dp)
        ) {
            drawCircle(
                color = currentColor,
            )
            drawCircle(
                color = outlineColor,
                style = Stroke(1.dp.toPx())
            )
        }
    }
    if (isExpanded) {
        Popup(
            properties = PopupProperties(
                focusable = true,
            ),
            onDismissRequest = { isExpanded = false },
        ) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(32.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .sizeIn(minWidth = 32.dp)
                                .clickable(
                                    onClick = {
                                        onChangeColor(color)
                                        isExpanded = false
                                    }
                                )
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                drawCircle(
                                    color = color,
                                )
                                drawCircle(
                                    color = outlineColor,
                                    style = Stroke(1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewColorSelector() = AlbumTheme {
    var color by remember { mutableStateOf(Color.Red) }
    ColorSelector(
        currentColor = color,
        onChangeColor = { color = it },
    )
}