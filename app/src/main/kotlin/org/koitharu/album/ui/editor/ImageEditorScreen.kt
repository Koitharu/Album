package org.koitharu.album.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButton
import androidx.compose.material3.SplitButtonDefaults.LeadingButton
import androidx.compose.material3.SplitButtonDefaults.TrailingButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.R
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.editor.ImageEditorIntent.Apply
import org.koitharu.album.ui.editor.ImageEditorIntent.Crop
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipHorizontal
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipVertical
import org.koitharu.album.ui.editor.ImageEditorIntent.ImageLoadFailed
import org.koitharu.album.ui.editor.ImageEditorIntent.Redo
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveCopy
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveReplacing
import org.koitharu.album.ui.editor.ImageEditorIntent.Share
import org.koitharu.album.ui.editor.ImageEditorIntent.ToggleMode
import org.koitharu.album.ui.editor.ImageEditorIntent.Undo
import org.koitharu.album.ui.editor.ImageEditorMode.CROP
import org.koitharu.album.ui.editor.ImageEditorMode.DRAW_ARROW
import org.koitharu.album.ui.editor.ImageEditorMode.DRAW_FREE
import org.koitharu.album.ui.editor.ImageEditorMode.MIRROR
import org.koitharu.album.ui.editor.ImageEditorMode.ROTATE
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.IconToggleButtonWithTooltip

@Composable
fun ImageEditorScreen(
    snackbarHostState: SnackbarHostState,
    state: ImageEditorState,
    handleIntent: MviIntentHandler<ImageEditorIntent>,
    onClose: () -> Unit,
) {
    var isCloseDialogVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.edit),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                subtitle = {
                    state.imageName?.let { subtitle ->
                        Text(
                            text = subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButtonWithTooltip(
                        tooltip = stringResource(R.string.close),
                        onClick = {
                            if (state.operations.isEmpty()) {
                                onClose()
                            } else {
                                isCloseDialogVisible = true
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    var isExpanded by remember { mutableStateOf(false) }
                    Box {
                        SplitButton(
                            leadingButton = {
                                LeadingButton(
                                    enabled = state.operations.isNotEmpty() && !state.isSaving,
                                    onClick = { handleIntent(SaveCopy) }
                                ) {
                                    Text(
                                        text = stringResource(R.string.save_copy)
                                    )
                                }
                            },
                            trailingButton = {
                                TrailingButton(
                                    enabled = state.operations.isNotEmpty() && !state.isSaving,
                                    checked = isExpanded,
                                    onCheckedChange = { isExpanded = it },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_drop_down),
                                        contentDescription = null,
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.save_copy),
                                        )
                                        Text(
                                            text = stringResource(R.string.save_copy_description),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                onClick = {
                                    isExpanded = false
                                    handleIntent(SaveCopy)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.replace),
                                        )
                                        Text(
                                            text = stringResource(R.string.replace_description),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                onClick = {
                                    isExpanded = false
                                    handleIntent(SaveReplacing)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.share),
                                        )
                                        Text(
                                            text = stringResource(R.string.share_description),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                },
                                onClick = {
                                    isExpanded = false
                                    handleIntent(Share)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = state.canApply,
                    ) {
                        FloatingActionButton(
                            onClick = { handleIntent(Apply) },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.apply),
                            )
                        }
                    }
                },
                actions = {
                    IconButtonWithTooltip(
                        tooltip = stringResource(R.string.undo),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        enabled = state.operations.isNotEmpty(),
                        onClick = { handleIntent(Undo) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.undo),
                        )
                    }
                    IconButtonWithTooltip(
                        tooltip = stringResource(R.string.redo),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        enabled = state.undoneOperations.isNotEmpty(),
                        onClick = { handleIntent(Redo) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_redo),
                            contentDescription = stringResource(R.string.redo),
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier.height(38.dp)
                    )
                    IconToggleButtonWithTooltip(
                        tooltip = stringResource(R.string.crop),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        checked = state.mode == CROP,
                        onCheckedChange = { handleIntent(ToggleMode(CROP)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_crop),
                            contentDescription = stringResource(R.string.crop),
                        )
                    }
                    IconToggleButtonWithTooltip(
                        tooltip = stringResource(R.string.rotate),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        checked = state.mode == ROTATE,
                        onCheckedChange = { handleIntent(ToggleMode(ROTATE)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rotate_90),
                            contentDescription = stringResource(R.string.rotate),
                        )
                    }
                    IconToggleButtonWithTooltip(
                        tooltip = stringResource(R.string.mirror),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        checked = state.mode == MIRROR,
                        onCheckedChange = { handleIntent(ToggleMode(MIRROR)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flip_horizontal),
                            contentDescription = stringResource(R.string.mirror),
                        )
                    }
                    IconToggleButtonWithTooltip(
                        tooltip = stringResource(R.string.draw_arrow),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        checked = state.mode == DRAW_ARROW,
                        onCheckedChange = { handleIntent(ToggleMode(DRAW_ARROW)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_target),
                            contentDescription = stringResource(R.string.draw_arrow),
                        )
                    }
                    IconToggleButtonWithTooltip(
                        tooltip = stringResource(R.string.free_draw),
                        tooltipAnchorPosition = TooltipAnchorPosition.Above,
                        checked = state.mode == DRAW_FREE,
                        onCheckedChange = { handleIntent(ToggleMode(DRAW_FREE)) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_draw),
                            contentDescription = stringResource(R.string.free_draw),
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            val imagePadding = WindowInsets.safeGestures.asPaddingValues() + PaddingValues(8.dp)
            var imageSize by remember { mutableStateOf<Size?>(null) }
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(imagePadding),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.imageUri)
                    .transformations(state.operations)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { s ->
                    (s as? AsyncImagePainter.State.Error)?.result?.throwable?.let { error ->
                        handleIntent(ImageLoadFailed(error))
                    }
                    val image = (s as? AsyncImagePainter.State.Success)?.result?.image
                    imageSize = image?.let {
                        Size(it.width.toFloat(), it.height.toFloat())
                    }
                }
            )
            imageSize?.let { size ->
                when (state.mode) {
                    CROP -> CropGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(imagePadding)
                            .aspectRatio(size.width / size.height),
                        contentPadding = PaddingValues.Zero,
                        lineColor = MaterialTheme.colorScheme.primary,
                        dimColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                        frame = state.cropFrame,
                        onFrameChanged = { frame ->
                            handleIntent(Crop(frame = frame))
                        },
                    )

                    MIRROR -> HorizontalFloatingToolbar(
                        expanded = true,
                    ) {
                        IconButtonWithTooltip(
                            tooltip = stringResource(R.string.mirror_horizontal),
                            tooltipAnchorPosition = TooltipAnchorPosition.Above,
                            onClick = { handleIntent(FlipHorizontal) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flip_horizontal),
                                contentDescription = stringResource(R.string.mirror_horizontal),
                            )
                        }
                        IconButtonWithTooltip(
                            tooltip = stringResource(R.string.mirror_vertical),
                            tooltipAnchorPosition = TooltipAnchorPosition.Above,
                            onClick = { handleIntent(FlipVertical) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flip_vertical),
                                contentDescription = stringResource(R.string.mirror_vertical),
                            )
                        }
                    }

                    null -> Unit
                    else -> Text(
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.medium,
                        ),
                        text = "Not yet implemented",
                    )
                }
            } ?: LoadingIndicator()
        }
    }
    BackHandler(
        enabled = state.operations.isNotEmpty()
    ) {
        isCloseDialogVisible = true
    }
    if (isCloseDialogVisible) {
        CloseConfirmDialog(
            onDismissRequest = { isCloseDialogVisible = false },
            onClose = onClose,
            handleIntent = handleIntent,
        )
    }
}

@Composable
private fun CloseConfirmDialog(
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    handleIntent: MviIntentHandler<ImageEditorIntent>,
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
        Text(
            text = stringResource(R.string.close)
        )
    },
    text = {
        Text(
            text = stringResource(R.string.editor_close_message)
        )
    },
    confirmButton = {
        TextButton(onClick = {
            onDismissRequest()
        }) { Text(stringResource(R.string.continue_editing)) }
    },
    dismissButton = {
        TextButton(onClick = {
            onClose()
        }) { Text(stringResource(R.string.discard_changes)) }
    }
)

@Preview
@Composable
private fun PreviewImageEditorScreen() = AlbumTheme {
    ImageEditorScreen(
        snackbarHostState = SnackbarHostState(),
        state = ImageEditorState(
            imageUri = "stub",
            imageName = "image.png",
            mode = MIRROR,
            operations = persistentListOf(),
            undoneOperations = persistentListOf(),
            cropFrame = FrameOffset.Zero,
            isSaving = false,
        ),
        handleIntent = MviIntentHandler.NoOp,
        onClose = { /* no-op */ },
    )
}
