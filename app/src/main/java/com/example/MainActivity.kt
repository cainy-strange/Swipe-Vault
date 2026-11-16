package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.media.PermissionHelper
import com.example.data.model.AlbumItem
import com.example.ui.navigation.AlbumsRoute
import com.example.ui.navigation.DeletionQueueRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.StatsRoute
import com.example.ui.navigation.SwipeRoute
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.DeletionQueueScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SwipeScreen
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrimsonDelete
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.SwipeVaultTheme
import com.example.ui.viewmodel.DeletionQueueViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.SwipeSessionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val appContainer = (application as SwipeVaultApp).container
        setContent {
            SwipeVaultTheme {
                SwipeVaultRoot(appContainer = appContainer)
            }
        }
    }
}

private data class TopLevelNavItem(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun SwipeVaultRoot(appContainer: AppContainer) {
    val navController = rememberNavController()
    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.Factory(
            appContext = appContainer.let { LocalContextApp() },
            repository = appContainer.swipeRepository
        )
    )
    val deletionQueueViewModel: DeletionQueueViewModel = viewModel(
        factory = DeletionQueueViewModel.Factory(appContainer.swipeRepository)
    )

    val galleryState by galleryViewModel.uiState.collectAsStateWithLifecycle()
    val stats by galleryViewModel.cleaningStats.collectAsStateWithLifecycle()
    val recentSessions by galleryViewModel.recentSessions.collectAsStateWithLifecycle()
    val deletionHistory by galleryViewModel.deletionHistory.collectAsStateWithLifecycle()
    val queuedCount by galleryViewModel.queuedDeletionCount.collectAsStateWithLifecycle()
    val deletionUiState by deletionQueueViewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        galleryViewModel.refreshGallery()
    }

    // Refresh permissions & MediaStore state on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                galleryViewModel.refreshGallery()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestFullOrInitialPermission = {
        permissionLauncher.launch(PermissionHelper.requiredPermissions())
    }

    val requestSelectMorePhotos = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        } else {
            permissionLauncher.launch(PermissionHelper.requiredPermissions())
        }
    }

    val topLevelItems = listOf(
        TopLevelNavItem(
            label = "Home",
            route = HomeRoute,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        TopLevelNavItem(
            label = "Albums",
            route = AlbumsRoute,
            selectedIcon = Icons.Rounded.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
            testTag = "nav_albums"
        ),
        TopLevelNavItem(
            label = "Queue",
            route = DeletionQueueRoute,
            selectedIcon = Icons.Rounded.DeleteSweep,
            unselectedIcon = Icons.Outlined.DeleteSweep,
            testTag = "nav_queue"
        ),
        TopLevelNavItem(
            label = "Stats",
            route = StatsRoute,
            selectedIcon = Icons.Rounded.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            testTag = "nav_stats"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomChrome = currentDestination?.hasRoute<SwipeRoute>() != true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = if (showBottomChrome) WindowInsets.statusBars else WindowInsets(0),
            bottomBar = {
                if (showBottomChrome && !useNavRail) {
                    Column {
                        HorizontalDivider(color = HairlineBorder, thickness = 0.5.dp)
                        NavigationBar(
                            containerColor = CharcoalSurface,
                            contentColor = PureWhite,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            topLevelItems.forEach { item ->
                                val selected = when (item.route) {
                                    is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    is AlbumsRoute -> currentDestination?.hasRoute<AlbumsRoute>() == true
                                    is DeletionQueueRoute -> currentDestination?.hasRoute<DeletionQueueRoute>() == true
                                    is StatsRoute -> currentDestination?.hasRoute<StatsRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    modifier = Modifier.testTag(item.testTag),
                                    icon = {
                                        val iconVec = if (selected) item.selectedIcon else item.unselectedIcon
                                        if (item.route is DeletionQueueRoute && queuedCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = CrimsonDelete,
                                                        contentColor = PureWhite
                                                    ) {
                                                        Text("$queuedCount")
                                                    }
                                                }
                                            ) {
                                                Icon(iconVec, contentDescription = item.label)
                                            }
                                        } else {
                                            Icon(iconVec, contentDescription = item.label)
                                        }
                                    },
                                    label = { Text(item.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PureWhite,
                                        selectedTextColor = PureWhite,
                                        unselectedIconColor = MutedGrey,
                                        unselectedTextColor = MutedGrey,
                                        indicatorColor = CharcoalElevated
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomChrome && useNavRail) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        contentColor = PureWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        topLevelItems.forEach { item ->
                            val selected = when (item.route) {
                                is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                is AlbumsRoute -> currentDestination?.hasRoute<AlbumsRoute>() == true
                                is DeletionQueueRoute -> currentDestination?.hasRoute<DeletionQueueRoute>() == true
                                is StatsRoute -> currentDestination?.hasRoute<StatsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    val iconVec = if (selected) item.selectedIcon else item.unselectedIcon
                                    if (item.route is DeletionQueueRoute && queuedCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = CrimsonDelete,
                                                    contentColor = PureWhite
                                                ) {
                                                    Text("$queuedCount")
                                                }
                                            }
                                        ) {
                                            Icon(iconVec, contentDescription = item.label)
                                        }
                                    } else {
                                        Icon(iconVec, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PureWhite,
                                    selectedTextColor = PureWhite,
                                    unselectedIconColor = MutedGrey,
                                    unselectedTextColor = MutedGrey,
                                    indicatorColor = CharcoalElevated
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                galleryState = galleryState,
                                stats = stats,
                                recentSessions = recentSessions,
                                onRequestGalleryPermission = requestFullOrInitialPermission,
                                onSelectMorePhotos = requestSelectMorePhotos,
                                onStartAllPhotosCleaning = {
                                    navController.navigate(
                                        SwipeRoute(
                                            bucketId = AlbumItem.ALL_PHOTOS_BUCKET_ID,
                                            albumName = "All Photos"
                                        )
                                    )
                                },
                                onSelectAlbum = { album ->
                                    navController.navigate(
                                        SwipeRoute(
                                            bucketId = album.bucketId,
                                            albumName = album.name
                                        )
                                    )
                                },
                                onOpenAlbums = { navigateTopLevel(AlbumsRoute) },
                                onOpenDeletionQueue = { navigateTopLevel(DeletionQueueRoute) },
                                onOpenStats = { navigateTopLevel(StatsRoute) }
                            )
                        }

                        composable<AlbumsRoute> {
                            AlbumsScreen(
                                galleryState = galleryState,
                                onSearchQueryChange = galleryViewModel::onSearchQueryChanged,
                                onSortSelected = galleryViewModel::onSortOptionSelected,
                                onRefresh = galleryViewModel::refreshGallery,
                                onRequestPermission = requestFullOrInitialPermission,
                                onSelectMorePhotos = requestSelectMorePhotos,
                                onStartAlbumCleaning = { album ->
                                    navController.navigate(
                                        SwipeRoute(
                                            bucketId = album.bucketId,
                                            albumName = album.name
                                        )
                                    )
                                }
                            )
                        }

                        composable<SwipeRoute> { backStackEntry ->
                            val route: SwipeRoute = backStackEntry.toRoute()
                            val swipeViewModel: SwipeSessionViewModel = viewModel(
                                key = "swipe_${route.bucketId}_${route.revisitSkippedOnly}",
                                factory = SwipeSessionViewModel.Factory(
                                    bucketId = route.bucketId,
                                    albumName = route.albumName,
                                    revisitSkippedOnly = route.revisitSkippedOnly,
                                    repository = appContainer.swipeRepository
                                )
                            )
                            val swipeState by swipeViewModel.uiState.collectAsStateWithLifecycle()

                            SwipeScreen(
                                uiState = swipeState,
                                onBack = {
                                    galleryViewModel.refreshGallery()
                                    navController.popBackStack()
                                },
                                onSwipeDecision = swipeViewModel::swipeCurrentPhoto,
                                onUndo = swipeViewModel::undoLastSwipe,
                                onTapPhoto = swipeViewModel::openFullScreen,
                                onDismissFullScreen = swipeViewModel::closeFullScreen,
                                onRevisitSkipped = swipeViewModel::startRevisitingSkipped,
                                onOpenDeletionQueue = {
                                    navController.navigate(DeletionQueueRoute) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable<DeletionQueueRoute> {
                            DeletionQueueScreen(
                                uiState = deletionUiState,
                                events = deletionQueueViewModel.events,
                                onToggleSelect = deletionQueueViewModel::toggleSelect,
                                onSelectAll = deletionQueueViewModel::selectAll,
                                onDeselectAll = deletionQueueViewModel::deselectAll,
                                onRemoveIndividual = deletionQueueViewModel::removeFromQueue,
                                onKeepSelected = deletionQueueViewModel::keepSelected,
                                onOpenPreview = deletionQueueViewModel::openPreview,
                                onClosePreview = deletionQueueViewModel::closePreview,
                                onOpenConfirmDialog = deletionQueueViewModel::openConfirmDialog,
                                onDismissConfirmDialog = deletionQueueViewModel::dismissConfirmDialog,
                                onConfirmOsDelete = deletionQueueViewModel::confirmAndStartOsDelete,
                                onOsDeleteResult = { approved ->
                                    deletionQueueViewModel.onOsDeleteResult(approved)
                                    if (approved) {
                                        galleryViewModel.refreshGallery()
                                    }
                                },
                                onDismissBanner = deletionQueueViewModel::dismissBanner,
                                onNavigateToAlbums = { navigateTopLevel(AlbumsRoute) }
                            )
                        }

                        composable<StatsRoute> {
                            StatsScreen(
                                stats = stats,
                                storageSnapshot = galleryState.storageSnapshot,
                                sessions = recentSessions,
                                deletionHistory = deletionHistory
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalContextApp() = androidx.compose.ui.platform.LocalContext.current.applicationContext
