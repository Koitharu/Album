package org.koitharu.album.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import org.koitharu.album.R
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.ui.OptionsMenuContent
import org.koitharu.album.ui.album.AlbumContent
import org.koitharu.album.ui.album.HomeScreenBanner
import org.koitharu.album.ui.common.LocalDarkMode
import org.koitharu.album.ui.common.OptionsMenu
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.common.SharedViewModelStoreNavEntryDecorator
import org.koitharu.album.ui.common.rememberSharedViewModelStoreNavEntryDecorator
import org.koitharu.album.ui.folders.FolderContentScreen
import org.koitharu.album.ui.folders.FoldersContent
import org.koitharu.album.ui.gallery.galleryViewModel
import org.koitharu.album.ui.home.NavDestination.HomeTab
import org.koitharu.album.ui.search.SearchBar
import org.koitharu.album.ui.search.SearchResults
import org.koitharu.album.ui.search.rememberSearchState
import org.koitharu.album.ui.viewer.ViewerScreen
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.ifThen
import org.koitharu.album.util.rememberNestedScrollDirectionConnection
import org.koitharu.album.util.setLast

@Composable
fun HomeNavHost() {
    val backStack = rememberSerializable(serializer = serializer()) {
        NavBackStack<NavDestination>(HomeTab.Album)
    }
    CompositionLocalProvider(LocalNavBackStack provides backStack) {
        SharedTransitionLayout {
            NavDisplay(
                backStack = backStack.ifEmpty { NavBackStack(HomeTab.Album) },
                sharedTransitionScope = this,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberSharedViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<HomeTab>(
                        clazzContentKey = { MediaQuery.DEFAULT.stringKey() },
                    ) { tab ->
                        HomeNavContent(
                            selectedTab = tab,
                        )
                    }
                    entry<NavDestination.FolderContent>(
                        clazzContentKey = { key -> key.folder.getMediaQuery().stringKey() },
                    ) {
                        FolderContentScreen(
                            folder = it.folder,
                        )
                    }
                    entry<NavDestination.MediaViewer>(
                        metadata = { key ->
                            SharedViewModelStoreNavEntryDecorator.parent(key.query.stringKey())
                        },
                    ) {
                        ViewerScreen(
                            query = it.query,
                            media = it.media,
                        )
                    }
                }
            )
        }
    }
}

@Composable
context(sharedTransitionScope: SharedTransitionScope)
fun HomeNavContent(
    selectedTab: HomeTab,
) {
    val backStack = LocalNavBackStack.current
    val scrollConnection = rememberNestedScrollDirectionConnection(4.dp)
    val scrollDirection by scrollConnection.direction
    val navState = rememberNavigationSuiteScaffoldState()
    val layoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val scope = rememberCoroutineScope()
    val searchState = rememberSearchState()
    BackHandler(searchState.isExpanded) {
        scope.launch { searchState.collapse() }
    }
    val albumViewModel = galleryViewModel(MediaQuery.DEFAULT)
    val state by albumViewModel.collectState()
    val isSelectionMode = state.selectedItems.isNotEmpty()
    val isNavigationVisible =
        layoutType == NavigationSuiteType.NavigationRail || (scrollDirection >= 0 && !isSelectionMode && searchState.isCollapsed)
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
                selected = selectedTab == HomeTab.Album,
                enabled = !isSelectionMode,
                onClick = { backStack.setLast(HomeTab.Album) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_album),
                        contentDescription = stringResource(R.string.app_name)
                    )
                },
                label = { Text(stringResource(R.string.app_name)) }
            )
            item(
                selected = selectedTab == HomeTab.Folders,
                enabled = !isSelectionMode,
                onClick = { backStack.setLast(HomeTab.Folders) },
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
        val banner = state.banner
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        var headerOffset by remember { mutableStateOf(0.dp) }
        val gridState = rememberLazyGridState()
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollConnection)
                .ifThen((banner == null || selectedTab != HomeTab.Album) && searchState.isCollapsed) {
                    nestedScroll(scrollBehavior.nestedScrollConnection)
                }
                .fillMaxSize(),
            topBar = {
                if (searchState.isExpanded) {
                    SearchBar(
                        modifier = Modifier
                            .background(TopAppBarDefaults.topAppBarColors().containerColor)
                            .statusBarsPadding(),
                        state = searchState,
                        onCollapse = { scope.launch { searchState.collapse() } },
                    )
                } else if (banner != null && selectedTab == HomeTab.Album) {
                    HomeScreenBanner(
                        banner = banner,
                        sharedTransitionScope = sharedTransitionScope,
                        onOffsetChanged = { headerOffset = it },
                        gridState = gridState,
                        overlayContent = {
                            OptionsMenu(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding(),
                                iconColor = if (state.isBannerDark) {
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
                        title = {
                            Text(stringResource(R.string.app_name))
                        },
                        actions = {
                            IconButtonWithTooltip(
                                tooltip = stringResource(R.string.search),
                                onClick = { scope.launch { searchState.expand() } },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                            OptionsMenu { onDismissRequest ->
                                OptionsMenuContent(onDismissRequest)
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalNavBackStack provides backStack) {
                if (searchState.isExpanded) {
                    SearchResults(
                        query = searchState.textFieldState.text.toString(),
                        contentPadding = innerPadding,
                    )
                } else {
                    when (selectedTab) {
                        HomeTab.Album -> with(sharedTransitionScope) {
                            AlbumContent(
                                query = MediaQuery.DEFAULT,
                                title = null,
                                gridState = gridState,
                                headerOffset = headerOffset,
                                innerPadding = innerPadding,
                            )
                        }

                        HomeTab.Folders -> FoldersContent(
                            innerPadding = innerPadding,
                        )
                    }
                }
            }
        }
        SetSystemBarsColorsEffect(
            isLightStatusBar = !if (selectedTab == HomeTab.Album && banner != null && searchState.isCollapsed) {
                state.isBannerDark
            } else {
                LocalDarkMode.current
            },
        )
    }
}
