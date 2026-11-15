package com.example.plume.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.data.repository.rememberTrackArtwork
import com.example.plume.domain.model.Track
import com.example.plume.ui.theme.BorderGlow

@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean = false,
    isCurrent: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    /** Shown for imported / on-device tracks only; deletes the file plus the DB row. */
    onDeleteLocal: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onClick() },
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        else Color(0xFF101014).copy(alpha = 0.85f),
        shape = cardShape,
        border = BorderStroke(
            1.dp,
            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else BorderGlow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isCurrent) 0.08f else 0.04f),
                            Color.White.copy(alpha = if (isCurrent) 0.03f else 0.01f)
                        )
                    )
                )
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Cover
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val artwork = rememberTrackArtwork(track)
                if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.50f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingEqualizerBars(
                            isPlaying = isPlaying,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title and Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Duration text
            Text(
                text = formatDuration(track.durationSeconds),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Options Menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF14141A))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.isFavorite) {
                                    PlumeStrings.get("removeFromFavorites", currentLanguage)
                                } else {
                                    PlumeStrings.get("addToFavorites", currentLanguage)
                                },
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (track.isFavorite) Color(0xFFFF4B6E) else Color.White
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onFavoriteToggle()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.isDownloaded) {
                                    PlumeStrings.get("removeDownload", currentLanguage)
                                } else {
                                    PlumeStrings.get("downloadAction", currentLanguage)
                                },
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (track.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDownloadToggle()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                PlumeStrings.get("addToPlaylist", currentLanguage),
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )

                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    PlumeStrings.get("removeFromPlaylist", currentLanguage),
                                    color = Color(0xFFFF4B6E)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4B6E)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            }
                        )
                    }

                    // Imported / on-device tracks can be removed from the device entirely.
                    if (track.isLocalFile) {
                        onDeleteLocal?.let { deleteLocal ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        PlumeStrings.get("deleteLocalTrack", currentLanguage),
                                        color = Color(0xFFFF4B6E)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF4B6E)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    deleteLocal()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

