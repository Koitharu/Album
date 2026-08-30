package org.koitharu.album.ui.info

import android.util.Size
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.koitharu.album.R
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.theme.AlbumTheme

@Composable
fun MediaInfoBottomSheet(
    image: AlbumItem.Image,
    onDismissRequest: () -> Unit,
) {
    val viewModel = hiltViewModel<MediaInfoViewModel, MediaInfoViewModel.Factory> {
        it.create(image)
    }
    val state by viewModel.collectState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        MediaInfoContent(
            state = state,
        )
    }
}

@Composable
private fun ColumnScope.MediaInfoContent(
    state: MediaInfoState
) {
    TableRow(
        title = stringResource(R.string.name),
        content = state.fileName,
    )
    TableRow(
        title = stringResource(R.string.width),
        content = state.size?.width?.toString(),
    )
    TableRow(
        title = stringResource(R.string.height),
        content = state.size?.height?.toString(),
    )
    TableRow(
        title = stringResource(R.string.resolution),
        content = state.sizeMp?.let {
            stringResource(R.string.size_megapixels_template, it)
        },
    )
    TableRow(
        title = stringResource(R.string.camera),
        content = state.model,
    )
    TableRow(
        title = stringResource(R.string.location),
        content = state.latLng?.let {
            "${it.first},${it.second}"
        },
    )
    HistogramBlock(
        modifier = Modifier
            .padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .fillMaxWidth()
            .aspectRatio(1.6f),
        data = state.histogram?.luminance,
        color = LocalContentColor.current,
    )
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HistogramBlock(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1.6f),
            data = state.histogram?.red,
            color = Color.Red,
        )
        HistogramBlock(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1.6f),
            data = state.histogram?.green,
            color = Color.Green,
        )
        HistogramBlock(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1.6f),
            data = state.histogram?.blue,
            color = Color.Blue,
        )
    }
}

@Composable
private fun HistogramBlock(
    modifier: Modifier,
    data: HistogramData?,
    color: Color,
) = AnimatedContent(
    modifier = modifier,
    targetState = data,
    transitionSpec = {
        fadeIn() togetherWith fadeOut()
    },
) { histogram ->
    Histogram(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = color,
        histogram = histogram ?: HistogramData.Empty,
    )
}

@Composable
private fun TableRow(
    title: String,
    content: String?,
) = if (!content.isNullOrEmpty()) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier
                .alignByBaseline()
                .weight(2f),
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            modifier = Modifier
                .alignByBaseline()
                .weight(3f),
            text = content,
            style = MaterialTheme.typography.bodySmall,
        )
    }
} else {
    Unit
}

@Composable
@Preview
private fun PreviewMediaInfoContent() = AlbumTheme {
    Column {
        MediaInfoContent(
            state = MediaInfoState(
                fileName = "013042.jpg",
                latLng = 32.04234 to 34.1231,
                model = "Camera 40000",
                size = Size(1600, 900),
                histogram = null,
            )
        )
    }
}