package org.koitharu.album.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumContent
import org.koitharu.album.ui.album.AlbumScope
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.common.AlbumItem.Media
import org.koitharu.album.ui.folders.FolderContentScreen
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.folders.FoldersContent
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.viewer.ViewerScreen
import org.koitharu.album.util.rememberPermissionCheck
import org.koitharu.album.util.rememberPermissionsCheck

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
                }
            }
        }
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
                        albumScope = AlbumScope(
                            gridState = gridState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        ),
                        onFolderClick = { selectedFolder = it },
                        onNavigationClick = { selectedTab = it }
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
    albumScope: AlbumScope,
    onNavigationClick: (Int) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onNavigationClick(0) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_album),
                            contentDescription = stringResource(R.string.app_name)
                        )
                    },
                    label = { Text(stringResource(R.string.app_name)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
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
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> AlbumContent(
                folder = null,
                innerPadding = innerPadding,
                albumScope = albumScope
            )

            1 -> FoldersContent(
                innerPadding = innerPadding,
                listState = foldersListState,
                onFolderClick = onFolderClick,
            )
        }
    }
}