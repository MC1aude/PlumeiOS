package com.example.plume.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.Playlist
import com.example.plume.domain.model.Track
import com.example.plume.ui.components.TrackItem
import com.example.plume.ui.theme.BorderGlow

data class LibraryTabItem(val key: String, val icon: ImageVector)

val libraryTabs = listOf(
    LibraryTabItem("playlists", Icons.AutoMirrored.Filled.QueueMusic),
    LibraryTabItem("onDevice", Icons.Default.PhoneAndroid),
    LibraryTabItem("favorites", Icons.Default.Favorite),
    LibraryTabItem("downloads", Icons.Default.DownloadDone)
)

@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    favorites: List<Track>,
    downloads: List<Track>,
    localTracks: List<Track>,
    playbackState: PlaybackState,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    isImportingAudio: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onCreatePlaylistClick: () -> Unit,
    onImportClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDeleteLocal: (Track) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar (Fixed status bar padding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = PlumeStrings.get("library", currentLanguage).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(start = if (onBackClick == null) 4.dp else 0.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onImportClick, enabled = !isImportingAudio) {
                    if (isImportingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = PlumeStrings.get("importFromPhone", currentLanguage),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = PlumeStrings.get("settings", currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            libraryTabs.forEachIndexed { index, tab ->
                val title = PlumeStrings.get(tab.key, currentLanguage)
                val selected = selectedTabIndex == index
                Tab(
                    selected = selected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = Color.White.copy(alpha = 0.50f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab content
        when (selectedTabIndex) {
            0 -> PlaylistsTabContent(
                playlists = playlists,
                currentLanguage = currentLanguage,
                isImportingAudio = isImportingAudio,
                onCreatePlaylistClick = onCreatePlaylistClick,
                onImportClick = onImportClick,
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylist = onDeletePlaylist
            )

            1 -> TrackListTabContent(
                tracks = localTracks,
                emptyText = PlumeStrings.get("noLocalTracks", currentLanguage),
                playbackState = playbackState,
                header = {
                    Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ImportTile(
                            isImporting = isImportingAudio,
                            currentLanguage = currentLanguage,
                            onClick = onImportClick
                        )
                    }
                },
                onTrackClick = onTrackClick,
                onFavoriteToggle = onFavoriteToggle,
                onDownloadToggle = onDownloadToggle,
                onAddToPlaylist = onAddToPlaylist,
                onDeleteLocal = onDeleteLocal,
                currentLanguage = currentLanguage
            )

            2 -> TrackListTabContent(
                tracks = favorites,
                emptyText = PlumeStrings.get("noFavorites", currentLanguage),
                playbackState = playbackState,
                onTrackClick = onTrackClick,
                onFavoriteToggle = onFavoriteToggle,
                onDownloadToggle = onDownloadToggle,
                onAddToPlaylist = onAddToPlaylist,
                onDeleteLocal = onDeleteLocal,
                currentLanguage = currentLanguage
            )

            3 -> TrackListTabContent(
                tracks = downloads,
                emptyText = PlumeStrings.get("noDownloads", currentLanguage),
                playbackState = playbackState,
                onTrackClick = onTrackClick,
                onFavoriteToggle = onFavoriteToggle,
                onDownloadToggle = onDownloadToggle,
                onAddToPlaylist = onAddToPlaylist,
                onDeleteLocal = onDeleteLocal,
                currentLanguage = currentLanguage
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Playlists Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlaylistsTabContent(
    playlists: List<Playlist>,
    currentLanguage: AppLanguage,
    isImportingAudio: Boolean = false,
    onCreatePlaylistClick: () -> Unit,
    onImportClick: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp)
    ) {
        item(key = "create_button") {
            Button(
                onClick = onCreatePlaylistClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = PlumeStrings.get("createPlaylist", currentLanguage),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "import_tile") {
            ImportTile(
                isImporting = isImportingAudio,
                currentLanguage = currentLanguage,
                onClick = onImportClick
            )
        }

        if (playlists.isEmpty()) {
            item(key = "empty_playlists") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PlumeStrings.get("noPlaylists", currentLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = playlists,
                key = { it.id },
                contentType = { "playlist" }
            ) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            )
                        )
                        .border(1.dp, BorderGlow, RoundedCornerShape(20.dp))
                        .clickable { onPlaylistClick(playlist) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = PlumeStrings.formatTrackCount(playlist.trackCount, currentLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }

                    IconButton(onClick = { onDeletePlaylist(playlist) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = PlumeStrings.get("deletePlaylistTitle", currentLanguage),
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportTile(
    isImporting: Boolean,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !isImporting) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isImporting) {
                    PlumeStrings.get("importing", currentLanguage)
                } else {
                    PlumeStrings.get("importFromPhone", currentLanguage)
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = "MP3 · M4A · FLAC · WAV · OGG",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.50f)
            )
        }

        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Track List Tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrackListTabContent(
    tracks: List<Track>,
    emptyText: String,
    playbackState: PlaybackState,
    onTrackClick: (Track, List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDeleteLocal: ((Track) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN
) {
    Column(modifier = Modifier.fillMaxSize()) {
        header?.invoke()

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 140.dp)
            ) {
                items(
                    items = tracks,
                    key = { it.id },
                    contentType = { "library_track" }
                ) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    Box(modifier = Modifier.padding(vertical = 3.dp)) {
                        TrackItem(
                            track = track,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            isCurrent = isCurrent,
                            currentLanguage = currentLanguage,
                            onClick = { onTrackClick(track, tracks) },
                            onFavoriteToggle = { onFavoriteToggle(track) },
                            onDownloadToggle = { onDownloadToggle(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onDeleteLocal = onDeleteLocal?.let { delete -> { delete(track) } }
                        )
                    }
                }
            }
        }
    }
}
