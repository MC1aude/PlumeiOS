package com.example.plume.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.plume.domain.model.Track
import com.example.plume.ui.components.AddToPlaylistBottomSheet
import com.example.plume.ui.components.BottomNavBar
import com.example.plume.ui.components.CreatePlaylistDialog
import com.example.plume.ui.components.DeletePlaylistDialog
import com.example.plume.ui.components.FullPlayerContent
import com.example.plume.ui.components.MiniPlayer
import com.example.plume.ui.components.NavTab
import com.example.plume.ui.components.PlumeToastBanner
import com.example.plume.ui.components.QueueBottomSheet
import com.example.plume.ui.components.SoundCloudFullPlayer
import com.example.plume.ui.components.SoundCloudMiniPlayer
import com.example.plume.ui.components.SplashScreen
import com.example.plume.ui.screens.home.HomeScreen
import com.example.plume.ui.screens.library.LibraryScreen
import com.example.plume.ui.screens.playlist.PlaylistDetailScreen
import com.example.plume.ui.screens.search.SearchScreen
import com.example.plume.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isSplashVisible by viewModel.isSplashVisible.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val isSoundCloudFullPlayerVisible by viewModel.isSoundCloudFullPlayerVisible.collectAsState()
    val isQueueVisible by viewModel.isQueueVisible.collectAsState()
    val selectedTrackForPlaylist by viewModel.selectedTrackForPlaylist.collectAsState()
    val isCreatePlaylistDialogVisible by viewModel.isCreatePlaylistDialogVisible.collectAsState()
    val playlistPendingDeletion by viewModel.playlistPendingDeletion.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()
    val isImportingAudio by viewModel.isImportingAudio.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val toastNotification by viewModel.toastNotification.collectAsState()

    val homeUiState by viewModel.homeViewModel.uiState.collectAsState()
    val soundCloudSearchState by viewModel.soundCloudSearchViewModel.uiState.collectAsState()
    val playlists by viewModel.libraryViewModel.playlists.collectAsState()
    val favorites by viewModel.libraryViewModel.favorites.collectAsState()
    val downloads by viewModel.libraryViewModel.downloads.collectAsState()
    val localTracks by viewModel.libraryViewModel.localTracks.collectAsState()

    val playlistUiState by viewModel.playlistViewModel.uiState.collectAsState()
    val settingsUiState by viewModel.settingsViewModel.uiState.collectAsState()

    val currentPlayingSong by viewModel.soundCloudPlayerViewModel.currentPlayingSong.collectAsState()
    val isSongPlaying by viewModel.soundCloudPlayerViewModel.isPlaying.collectAsState()
    val isSongLoading by viewModel.soundCloudPlayerViewModel.isLoading.collectAsState()
    val songPositionMs by viewModel.soundCloudPlayerViewModel.positionMs.collectAsState()
    val songDurationMs by viewModel.soundCloudPlayerViewModel.durationMs.collectAsState()

    val isMainTabRoute = currentRoute in listOf(NavTab.Home.route, NavTab.Search.route, NavTab.Library.route)
    val anyFullPlayerVisible = isFullPlayerVisible || isSoundCloudFullPlayerVisible

    // ── Local file import (SAF) ──
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importAudio(it) }
    }
    val launchImport = { importLauncher.launch(arrayOf("audio/*")) }

    // ── Storage permission for pre-Android 10 public Downloads writes ──
    var pendingDownload by remember { mutableStateOf<Track?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val track = pendingDownload
        pendingDownload = null
        if (granted && track != null) {
            viewModel.downloadTrack(track)
        } else if (!granted) {
            viewModel.notifyDownloadPermissionDenied()
        }
    }

    val requestDownload: (Track) -> Unit = { track ->
        val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsLegacyPermission && !track.isDownloaded) {
            pendingDownload = track
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.downloadTrack(track)
        }
    }

    LaunchedEffect(uiMessage) {
        val message = uiMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = NavTab.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(NavTab.Home.route) {
                        HomeScreen(
                            uiState = homeUiState,
                            currentPlayingSong = currentPlayingSong,
                            isPlaying = isSongPlaying,
                            currentLanguage = settingsUiState.language,
                            onFilterSelected = { filter -> viewModel.homeViewModel.selectFilter(filter) },
                            onSongClick = { song -> viewModel.playSong(song) },
                            onRetry = { viewModel.homeViewModel.loadFeed() }
                        )
                    }

                    composable(NavTab.Search.route) {
                        SearchScreen(
                            uiState = soundCloudSearchState,
                            currentPlayingSong = currentPlayingSong,
                            isPlaying = isSongPlaying,
                            currentLanguage = settingsUiState.language,
                            onQueryChange = { viewModel.soundCloudSearchViewModel.onQueryChange(it) },
                            onSongClick = { song -> viewModel.playSong(song) }
                        )
                    }

                    composable(NavTab.Library.route) {
                        LibraryScreen(
                            playlists = playlists,
                            favorites = favorites,
                            downloads = downloads,
                            localTracks = localTracks,
                            playbackState = playbackState,
                            currentLanguage = settingsUiState.language,
                            isImportingAudio = isImportingAudio,
                            onSettingsClick = {
                                viewModel.settingsViewModel.loadStorageInfo()
                                navController.navigate("settings")
                            },
                            onCreatePlaylistClick = { viewModel.showCreatePlaylistDialog() },
                            onImportClick = launchImport,
                            onPlaylistClick = { playlist ->
                                viewModel.openPlaylist(playlist)
                                navController.navigate("playlist_detail")
                            },
                            onDeletePlaylist = { viewModel.requestDeletePlaylist(it) },
                            onTrackClick = { track, queue ->
                                viewModel.playTrack(track, queue)
                                viewModel.showFullPlayer()
                            },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onDownloadToggle = { requestDownload(it) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onDeleteLocal = { viewModel.deleteLocalTrack(it) }
                        )
                    }

                    composable("playlist_detail") {
                        PlaylistDetailScreen(
                            uiState = playlistUiState,
                            playbackState = playbackState,
                            currentLanguage = settingsUiState.language,
                            onBackClick = { navController.popBackStack() },
                            onTrackClick = { track, queue ->
                                viewModel.playTrack(track, queue)
                                viewModel.showFullPlayer()
                            },
                            onPlayAll = { queue ->
                                viewModel.playQueue(queue)
                                viewModel.showFullPlayer()
                            },
                            onFavoriteToggle = { viewModel.toggleFavorite(it) },
                            onDownloadToggle = { requestDownload(it) },
                            onAddToPlaylist = { viewModel.openAddToPlaylist(it) },
                            onRemoveFromPlaylist = { track ->
                                viewModel.playlistViewModel.removeTrackFromPlaylist(track.id)
                            },
                            onDeleteLocal = { viewModel.deleteLocalTrack(it) }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            uiState = settingsUiState,
                            onBackClick = { navController.popBackStack() },
                            onSelectLanguage = { lang -> viewModel.settingsViewModel.selectLanguage(lang) },
                            onClearCache = { viewModel.settingsViewModel.clearCache() },
                            onClearDownloads = { viewModel.settingsViewModel.clearDownloads() },
                            onClearMessage = { viewModel.settingsViewModel.clearMessage() }
                        )
                    }
                }

                // ── Floating Bottom Navigation & Single Mini-Player ──
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Exactly ONE instance of the mini-player is rendered conditionally
                    AnimatedVisibility(
                        visible = !anyFullPlayerVisible &&
                            (currentPlayingSong != null || playbackState.currentTrack != null),
                        enter = slideInVertically(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = slideOutVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetY = { it / 2 }
                        ) + fadeOut(animationSpec = tween(160))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (isMainTabRoute) 4.dp else 12.dp)
                        ) {
                            if (currentPlayingSong != null) {
                                SoundCloudMiniPlayer(
                                    viewModel = viewModel.soundCloudPlayerViewModel,
                                    onOpenFullPlayer = { viewModel.showSoundCloudFullPlayer() }
                                )
                            } else if (playbackState.currentTrack != null) {
                                MiniPlayer(
                                    playbackState = playbackState,
                                    onTogglePlayPause = { viewModel.toggleTrackPlayPause() },
                                    onNext = { viewModel.next() },
                                    onClick = { viewModel.showFullPlayer() }
                                )
                            }
                        }
                    }

                    // Floating Bottom Nav Bar
                    AnimatedVisibility(
                        visible = isMainTabRoute && !anyFullPlayerVisible,
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(NavTab.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Track-based Now Playing (edge-to-edge, above the status bar) ──
        AnimatedVisibility(
            visible = isFullPlayerVisible,
            enter = slideInVertically(
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerContent(
                playbackState = playbackState,
                currentLanguage = settingsUiState.language,
                downloadingTrackIds = downloadingTrackIds,
                onDismiss = { viewModel.hideFullPlayer() },
                onTogglePlayPause = { viewModel.toggleTrackPlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onFavoriteToggle = {
                    playbackState.currentTrack?.let { track -> viewModel.toggleFavorite(track) }
                },
                onDownloadToggle = { track -> requestDownload(track) },
                onAddToPlaylist = {
                    playbackState.currentTrack?.let { track -> viewModel.openAddToPlaylist(track) }
                },
                onOpenQueue = { viewModel.showQueue() }
            )
        }

        // ── SoundCloud Now Playing ──
        AnimatedVisibility(
            visible = isSoundCloudFullPlayerVisible,
            enter = slideInVertically(
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            currentPlayingSong?.let { song ->
                val track = remember(song) {
                    Track(
                        id = song.id,
                        title = song.title,
                        artistName = song.artist,
                        albumName = "",
                        coverUrl = song.thumbnailUrl,
                        audioUrl = song.streamEndpoint,
                        downloadUrl = song.streamEndpoint,
                        durationSeconds = 0
                    )
                }
                val isFav = favorites.any { it.id == track.id }
                val isDown = downloads.any { it.id == track.id }
                val isDowning = track.id in downloadingTrackIds

                SoundCloudFullPlayer(
                    song = song,
                    isPlaying = isSongPlaying,
                    isLoading = isSongLoading,
                    positionMs = songPositionMs,
                    durationMs = songDurationMs,
                    currentLanguage = settingsUiState.language,
                    isFavorite = isFav,
                    isDownloaded = isDown,
                    isDownloading = isDowning,
                    onDismiss = { viewModel.hideSoundCloudFullPlayer() },
                    onTogglePlayPause = { viewModel.soundCloudPlayerViewModel.togglePlayPause() },
                    onNext = { viewModel.soundCloudPlayerViewModel.playNext() },
                    onPrevious = { viewModel.soundCloudPlayerViewModel.playPrevious() },
                    onSeekTo = { viewModel.soundCloudPlayerViewModel.seekTo(it) },
                    onFavoriteToggle = { viewModel.toggleFavorite(track.copy(isFavorite = isFav)) },
                    onDownloadToggle = { requestDownload(track.copy(isDownloaded = isDown)) },
                    onAddToPlaylist = { viewModel.openAddToPlaylist(track) }
                )
            }
        }

        // ── Queue Bottom Sheet ──
        if (isQueueVisible) {
            QueueBottomSheet(
                queue = playbackState.queue,
                currentTrack = playbackState.currentTrack,
                currentLanguage = settingsUiState.language,
                onDismiss = { viewModel.hideQueue() },
                onTrackSelect = { track -> viewModel.playTrack(track, playbackState.queue) },
                onRemoveFromQueue = { track -> viewModel.removeFromQueue(track.id) }
            )
        }

        // ── Add To Playlist Bottom Sheet ──
        selectedTrackForPlaylist?.let { track ->
            AddToPlaylistBottomSheet(
                playlists = playlists,
                track = track,
                currentLanguage = settingsUiState.language,
                onDismiss = { viewModel.closeAddToPlaylist() },
                onPlaylistSelected = { playlist ->
                    viewModel.addTrackToPlaylist(playlist, track)
                },
                onCreateNewPlaylist = {
                    viewModel.showCreatePlaylistDialog()
                }
            )
        }

        // ── Create Playlist Dialog ──
        if (isCreatePlaylistDialogVisible) {
            CreatePlaylistDialog(
                currentLanguage = settingsUiState.language,
                onDismiss = { viewModel.hideCreatePlaylistDialog() },
                onCreate = { name -> viewModel.createPlaylist(name) }
            )
        }

        // ── Delete Playlist Confirmation (destructive, requires explicit "Так") ──
        playlistPendingDeletion?.let { playlist ->
            DeletePlaylistDialog(
                playlistName = playlist.name,
                currentLanguage = settingsUiState.language,
                onDismiss = { viewModel.dismissDeletePlaylist() },
                onConfirm = { viewModel.confirmDeletePlaylist() }
            )
        }

        // ── Custom Glassmorphism Toast Notification Banner ──
        PlumeToastBanner(
            message = toastNotification?.message,
            icon = toastNotification?.icon ?: Icons.Default.Favorite,
            accent = toastNotification?.accent ?: Color.White,
            onDismiss = { viewModel.clearToast() }
        )

        // ── Cinematic Startup Splash Overlay ──
        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 1.05f, animationSpec = tween(600, easing = FastOutSlowInEasing)),
            modifier = Modifier.fillMaxSize()
        ) {
            SplashScreen(
                onSplashFinished = { viewModel.dismissSplash() }
            )
        }
    }
}
