package org.koitharu.album.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumContent
import org.koitharu.album.ui.album.AlbumIntent.HandleClick
import org.koitharu.album.ui.album.AlbumScope
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.album.HomeScreenBanner
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.AlbumItem.Media
import org.koitharu.album.ui.common.ComposeActivity
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.common.LocalDarkMode
import org.koitharu.album.ui.common.OptionsMenu
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.folders.FolderContentScreen
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.folders.FoldersContent
import org.koitharu.album.ui.settings.SettingsActivity
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.viewer.ViewerScreen
import org.koitharu.album.util.ifThen
import org.koitharu.album.util.rememberNestedScrollDirectionConnection
import org.koitharu.album.util.rememberPermissionCheck
import org.koitharu.album.util.rememberPermissionsCheck

@AndroidEntryPoint
class MainActivity : ComposeActivity() {

    @Composable
    override fun Content() {
        AlbumTheme {
            val isPermissionGranted by
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                rememberPermissionsCheck(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                rememberPermissionCheck(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            if (isPermissionGranted) {
                HomeScreen()
            } else {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    iconResId = R.drawable.ic_folder_alert,
                    title = stringResource(R.string.no_permissions),
                    message = stringResource(R.string.no_permissions_message),
                ) {
                    Button(
                        onClick = { openAppSettings() }
                    ) {
                        Text(
                            text = stringResource(R.string.settings)
                        )
                    }
                }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun HomeScreen() {
    var selectedFolder by rememberSaveable { mutableStateOf<FolderItem?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val foldersListState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    selectedFolder?.let { folder ->
        FolderContentScreen(
            folder = folder,
            onClose = { selectedFolder = null }
        )
    } ?: run {
        val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory> {
            it.create(null)
        }
        val state by viewModel.collectState()
        SharedTransitionLayout {
            AnimatedContent(state.openedItem) { openedItem ->
                when (openedItem) {
                    is Media -> ViewerScreen(
                        folder = null,
                        media = openedItem,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )

                    null -> HomeContent(
                        selectedTab = selectedTab,
                        foldersListState = foldersListState,
                        banner = state.banner,
                        isBannerDark = state.isBannerDark,
                        isSelectionMode = state.selectedItems.isNotEmpty(),
                        albumScope = AlbumScope(
                            gridState = gridState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            headerOffset = remember { mutableStateOf(0.dp) },
                        ),
                        onFolderClick = { selectedFolder = it },
                        onNavigationClick = { selectedTab = it },
                        onBannerClick = { viewModel.handleIntent(HandleClick(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    selectedTab: Int,
    foldersListState: LazyListState,
    banner: AlbumItem.Image?,
    isBannerDark: Boolean,
    isSelectionMode: Boolean,
    albumScope: AlbumScope,
    onNavigationClick: (Int) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onBannerClick: (AlbumItem.Image) -> Unit,
) {
    val scrollConnection = rememberNestedScrollDirectionConnection(4.dp)
    val scrollDirection by scrollConnection.direction
    val navState = rememberNavigationSuiteScaffoldState()
    val layoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val isNavigationVisible =
        layoutType == NavigationSuiteType.NavigationRail || (scrollDirection >= 0 && !isSelectionMode)
    LaunchedEffect(isNavigationVisible) {
        if (isNavigationVisible) {
            navState.show()
        } else {
            navState.hide()
        }
    }
    NavigationSuiteScaffold(
        state = navState,
        layoutType = layoutType,
        navigationSuiteItems = {
            item(
                selected = selectedTab == 0,
                enabled = !isSelectionMode,
                onClick = { onNavigationClick(0) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_album),
                        contentDescription = stringResource(R.string.app_name)
                    )
                },
                label = { Text(stringResource(R.string.app_name)) }
            )
            item(
                selected = selectedTab == 1,
                enabled = !isSelectionMode,
                onClick = { onNavigationClick(1) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_folders),
                        contentDescription = stringResource(R.string.folders)
                    )
                },
                label = { Text(stringResource(R.string.folders)) }
            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollConnection)
                .ifThen(banner == null || selectedTab != 0) {
                    nestedScroll(scrollBehavior.nestedScrollConnection)
                }
                .fillMaxSize(),
            topBar = {
                if (banner != null && selectedTab == 0) {
                    HomeScreenBanner(
                        banner = banner,
                        albumScope = albumScope,
                        onClick = { onBannerClick(banner) },
                        overlayContent = {
                            OptionsMenu(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding(),
                                iconColor = if (isBannerDark) {
                                    Color.White
                                } else {
                                    Color.Black
                                },
                                content = { onDismissRequest ->
                                    OptionsMenuContent(onDismissRequest)
                                },
                            )
                        }
                    )
                } else {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            OptionsMenu { onDismissRequest ->
                                OptionsMenuContent(onDismissRequest)
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
        ) { innerPadding ->
            when (selectedTab) {
                0 -> AlbumContent(
                    folder = null,
                    innerPadding = innerPadding,
                    albumScope = albumScope,
                )

                1 -> FoldersContent(
                    innerPadding = innerPadding,
                    listState = foldersListState,
                    onFolderClick = onFolderClick,
                )
            }
        }
        SetSystemBarsColorsEffect(
            isLightStatusBar = !if (selectedTab == 0 && banner != null) {
                isBannerDark
            } else {
                LocalDarkMode.current
            },
        )
    }
}

@Composable
private fun OptionsMenuContent(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(R.string.settings)) },
        onClick = {
            onDismissRequest()
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    )
}