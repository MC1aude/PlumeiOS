package com.example.plume.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.Track
import com.example.plume.ui.components.TrackItem

@Composable
fun PlaylistDetailScreen(
    uiState: PlaylistDetailUiState,
    playbackState: PlaybackState,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRemoveFromPlaylist: ((Track) -> Unit)? = null,
    onDeleteLocal: ((Track) -> Unit)? = null
) {
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = uiState.playlistName.ifEmpty {
                        PlumeStrings.get("playlistShort", currentLanguage)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = PlumeStrings.formatTrackCount(uiState.tracks.size, currentLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PlumeStrings.get("emptyPlaylist", currentLanguage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 140.dp)
            ) {
                item(key = "play_all_button") {
                    Button(
                        onClick = { onPlayAll(uiState.tracks) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = PlumeStrings.get("playAll", currentLanguage),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(
                    items = uiState.tracks,
                    key = { it.id },
                    contentType = { "playlist_track" }
                ) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    Box(modifier = Modifier.padding(vertical = 3.dp)) {
                        TrackItem(
                            track = track,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            isCurrent = isCurrent,
                            currentLanguage = currentLanguage,
                            onClick = { onTrackClick(track, uiState.tracks) },
                            onFavoriteToggle = { onFavoriteToggle(track) },
                            onDownloadToggle = { onDownloadToggle(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onRemoveFromPlaylist = if (onRemoveFromPlaylist != null) {
                                { onRemoveFromPlaylist(track) }
                            } else null,
                            onDeleteLocal = onDeleteLocal?.let { delete -> { delete(track) } }
                        )
                    }
                }
            }
        }
    }
}
