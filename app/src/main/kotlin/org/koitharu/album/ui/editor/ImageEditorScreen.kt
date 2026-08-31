package org.koitharu.album.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.transformations
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.R
import org.koitharu.album.ui.common.Fraction
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.editor.ImageEditorIntent.Apply
import org.koitharu.album.ui.editor.ImageEditorIntent.Crop
import org.koitharu.album.ui.editor.ImageEditorIntent.Draw
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipHorizontal
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipVertical
import org.koitharu.album.ui.editor.ImageEditorIntent.ImageLoadFailed
import org.koitharu.album.ui.editor.ImageEditorIntent.Redo
import org.koitharu.album.ui.editor.ImageEditorIntent.Reset
import org.koitharu.album.ui.editor.ImageEditorIntent.Rotate
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveCopy
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveReplacing
import org.koitharu.album.ui.editor.ImageEditorIntent.SetColor
import org.koitharu.album.ui.editor.ImageEditorIntent.SetCropAspectRatio
import org.koitharu.album.ui.editor.ImageEditorIntent.SetLineThickness
import org.koitharu.album.ui.editor.ImageEditorIntent.SetMode
import org.koitharu.album.ui.editor.ImageEditorIntent.Share
import org.koitharu.album.ui.editor.ImageEditorIntent.Undo
import org.koitharu.album.ui.editor.ImageEditorMode.CROP
import org.koitharu.album.ui.editor.ImageEditorMode.DRAW_ARROW
import org.koitharu.album.ui.editor.ImageEditorMode.DRAW_FREE
import org.koitharu.album.ui.editor.ImageEditorMode.MIRROR
import org.koitharu.album.ui.editor.ImageEditorMode.ROTATE
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip

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
            BottomBar(state, handleIntent)
        }
    ) { innerPadding ->
        BoxWithConstraints(
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
                    .useExistingImageAsPlaceholder(true)
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
                val imageAspectRatio = size.width / size.height
                val boxAspectRatio = maxWidth.value / maxHeight.value
                when (state.mode) {
                    CROP -> CropGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(imagePadding)
                            .aspectRatio(imageAspectRatio, boxAspectRatio > imageAspectRatio),
                        lineColor = MaterialTheme.colorScheme.primary,
                        dimColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f),
                        frame = state.cropFrame,
                        aspectRatio = state.cropAspectRatio.toFloat(),
                        onFrameChanged = { frame ->
                            handleIntent(Crop(frame = frame))
                        },
                    )

                    DRAW_ARROW -> ArrowChalkboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(imagePadding)
                            .aspectRatio(imageAspectRatio, boxAspectRatio > imageAspectRatio),
                        arrow = state.currentArrow,
                        lineThickness = state.lineThickness,
                        currentColor = state.currentColor,
                        onArrowDrawn = { handleIntent(Draw(it)) },
                    )

                    DRAW_FREE -> FreeDrawChalkboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(imagePadding)
                            .aspectRatio(imageAspectRatio, boxAspectRatio > imageAspectRatio),
                        path = state.currentPath,
                        lineThickness = state.lineThickness,
                        currentColor = state.currentColor,
                        onPathDrawn = { handleIntent(Draw(it)) },
                    )

                    else -> Unit
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
        )
    }
}

@Composable
private fun BottomBar(
    state: ImageEditorState,
    handleIntent: MviIntentHandler<ImageEditorIntent>
) {
    BottomAppBar(
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.canApply,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
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
            AnimatedContent(
                targetState = state.mode,
                transitionSpec = {
                    slideIntoContainer(
                        towards = SlideDirection.End,
                    ) togetherWith slideOutOfContainer(
                        towards = SlideDirection.End,
                    )
                }
            ) { mode ->
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mode != null) {
                        IconButtonWithTooltip(
                            tooltip = stringResource(R.string.back),
                            tooltipAnchorPosition = TooltipAnchorPosition.Above,
                            onClick = { handleIntent(SetMode(null)) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(38.dp)
                                .padding(horizontal = 8.dp)
                        )
                    }
                    when (mode) {
                        DRAW_FREE,
                        DRAW_ARROW -> {
                            ColorSelector(
                                currentColor = state.currentColor,
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onChangeColor = { handleIntent(SetColor(it)) }
                            )
                            val density = LocalDensity.current
                            SpinnerButton(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp),
                                items = persistentListOf(1.dp, 2.dp, 4.dp, 6.dp, 8.dp, 10.dp),
                                selectedItem = state.lineThickness,
                                tooltip = stringResource(R.string.line_thickness),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onItemClick = {
                                    handleIntent(
                                        SetLineThickness(
                                            thickness = it,
                                            thicknessPx = with(density) { it.toPx() },
                                        )
                                    )
                                },
                            ) { thickness, isSelected ->
                                if (isSelected) {
                                    Icon(
                                        modifier = Modifier.padding(end = 8.dp),
                                        painter = painterResource(R.drawable.ic_line_weight),
                                        contentDescription = null,
                                    )
                                }
                                Text(
                                    text = thickness.value.toString().removeSuffix(".0"),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.delete),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                enabled = state.currentArrow != null,
                                onClick = { handleIntent(Reset) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_backspace),
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }

                        CROP -> {
                            SpinnerButton(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp),
                                items = persistentListOf(
                                    Fraction.Unspecified,
                                    Fraction(1, 1),
                                    Fraction(4, 3),
                                    Fraction(3, 4),
                                    Fraction(16, 9),
                                    Fraction(9, 16),
                                ),
                                selectedItem = state.cropAspectRatio,
                                tooltip = stringResource(R.string.aspect_ratio),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onItemClick = { handleIntent(SetCropAspectRatio(it)) },
                            ) { fraction, isSelected ->
                                if (isSelected) {
                                    Icon(
                                        modifier = Modifier.padding(end = 8.dp),
                                        painter = painterResource(R.drawable.ic_aspect_ratio),
                                        contentDescription = null,
                                    )
                                }
                                Text(
                                    text = if (fraction.isUnspecified()) {
                                        stringResource(R.string.free)
                                    } else {
                                        fraction.toString()
                                    },
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.reset),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                enabled = state.cropFrame != FrameOffset.Zero,
                                onClick = { handleIntent(Reset) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_backspace),
                                    contentDescription = stringResource(R.string.reset),
                                )
                            }
                        }

                        ROTATE -> {
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.rotate_ccw),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(Rotate(-90)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_rotate_ccw),
                                    contentDescription = stringResource(R.string.rotate_ccw),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.rotate_cw),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(Rotate(90)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_rotate_cw),
                                    contentDescription = stringResource(R.string.rotate_cw),
                                )
                            }
                        }

                        MIRROR -> {
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

                        null -> {
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
                                modifier = Modifier
                                    .height(38.dp)
                                    .padding(horizontal = 8.dp)
                            )
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.crop),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(SetMode(CROP)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_crop),
                                    contentDescription = stringResource(R.string.crop),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.rotate),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(SetMode(ROTATE)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_rotate_90),
                                    contentDescription = stringResource(R.string.rotate),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.mirror),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(SetMode(MIRROR)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_flip_horizontal),
                                    contentDescription = stringResource(R.string.mirror),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.draw_arrow),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(SetMode(DRAW_ARROW)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_target),
                                    contentDescription = stringResource(R.string.draw_arrow),
                                )
                            }
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.free_draw),
                                tooltipAnchorPosition = TooltipAnchorPosition.Above,
                                onClick = { handleIntent(SetMode(DRAW_FREE)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_draw),
                                    contentDescription = stringResource(R.string.free_draw),
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    )
}

@Composable
private fun CloseConfirmDialog(
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
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
            mode = DRAW_ARROW,
            operations = persistentListOf(),
            undoneOperations = persistentListOf(),
            cropFrame = FrameOffset.Zero,
            cropAspectRatio = Fraction(1, 1),
            currentArrow = null,
            currentPath = null,
            currentColor = Color.Red,
            lineThickness = 2.dp,
            isSaving = false,
        ),
        handleIntent = MviIntentHandler.NoOp,
        onClose = { /* no-op */ },
    )
}
