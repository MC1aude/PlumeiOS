package com.example.plume.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.plume.domain.model.Playlist
import com.example.plume.domain.model.Track
import com.example.plume.ui.theme.DarkCard
import com.example.plume.ui.theme.GlassHighlight

// ─────────────────────────────────────────────────────────────────────────────
// Create playlist dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CreatePlaylistDialog(
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = PlumeStrings.get("createPlaylist", currentLanguage),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(PlumeStrings.get("playlists", currentLanguage)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(
                    PlumeStrings.get("createPlaylist", currentLanguage),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "✕",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = DarkCard,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Delete playlist confirmation dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dark glassmorphism confirmation dialog used before a playlist is destroyed.
 * The playlist is only deleted when the user explicitly taps "Так".
 */
@Composable
fun DeletePlaylistDialog(
    playlistName: String = "",
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val destructiveAccent = Color(0xFFFF4B6E)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = DarkCard,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(destructiveAccent.copy(alpha = 0.10f))
                    .border(1.dp, destructiveAccent.copy(alpha = 0.32f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = destructiveAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = PlumeStrings.get("deletePlaylistTitle", currentLanguage),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = PlumeStrings.get("deletePlaylistMessage", currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )

                if (playlistName.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = PlumeStrings.get("confirmYes", currentLanguage),
                    color = destructiveAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = PlumeStrings.get("cancel", currentLanguage),
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Add to playlist bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    playlists: List<Playlist>,
    track: Track?,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E11),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
        ) {
            Text(
                text = PlumeStrings.get("addToPlaylist", currentLanguage),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            )

            if (track != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Create new
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onCreateNewPlaylist() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = PlumeStrings.get("createNewPlaylist", currentLanguage),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PlumeStrings.get("noPlaylists", currentLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        items = playlists,
                        key = { it.id },
                        contentType = { "playlist" }
                    ) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassHighlight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = PlumeStrings.formatTrackCount(playlist.trackCount, currentLanguage),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Queue bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Track>,
    currentTrack: Track?,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onDismiss: () -> Unit,
    onTrackSelect: (Track) -> Unit,
    onRemoveFromQueue: (Track) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E11),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = PlumeStrings.get("queueTitle", currentLanguage),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = PlumeStrings.formatTrackCount(queue.size, currentLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("✕", color = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(12.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PlumeStrings.get("emptyQueue", currentLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(
                        items = queue,
                        key = { index, track -> "${track.id}_$index" },
                        contentType = { _, _ -> "queue_track" }
                    ) { _, track ->
                        val isCurrent = currentTrack?.id == track.id
                        val artwork = rememberTrackArtwork(track)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isCurrent) Color.White.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable { onTrackSelect(track) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color.White.copy(alpha = 0.07f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (artwork != null) {
                                    AsyncImage(
                                        model = artwork,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artistName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveFromQueue(track) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = PlumeStrings.get("remove", currentLanguage),
                                    tint = Color.White.copy(alpha = 0.40f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
    )
}
