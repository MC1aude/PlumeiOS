package com.example.plume.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.example.plume.data.repository.rememberTrackArtwork
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.RepeatMode
import com.example.plume.domain.model.Song
import com.example.plume.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Full Player for the local Track-based player (PlayerController)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FullPlayerContent(
    playbackState: PlaybackState,
    currentLanguage: AppLanguage,
    downloadingTrackIds: Set<String>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadToggle: (Track) -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val artwork = rememberTrackArtwork(track)
    val accent = rememberDominantColor(artwork)

    NowPlayingScaffold(
        artwork = artwork,
        title = track.title,
        artist = track.artistName,
        accent = accent,
        label = PlumeStrings.get("nowPlaying", currentLanguage),
        onDismiss = onDismiss,
        positionMs = playbackState.positionMs,
        durationMs = playbackState.durationMs,
        isPlaying = playbackState.isPlaying,
        isLoading = playbackState.isLoading,
        isFavorite = track.isFavorite,
        isDownloaded = track.isDownloaded,
        isDownloading = track.id in downloadingTrackIds,
        favoriteLabel = if (track.isFavorite) {
            PlumeStrings.get("favoriteActiveShort", currentLanguage)
        } else {
            PlumeStrings.get("favoriteShort", currentLanguage)
        },
        playlistLabel = PlumeStrings.get("playlistShort", currentLanguage),
        downloadLabel = PlumeStrings.get("downloadShort", currentLanguage),
        downloadedLabel = PlumeStrings.get("savedShort", currentLanguage),
        onFavoriteToggle = onFavoriteToggle,
        onDownloadToggle = { onDownloadToggle(track) },
        onAddToPlaylist = onAddToPlaylist,
        onSeek = { fraction -> onSeekTo((fraction * playbackState.durationMs).toLong()) },
        onTogglePlayPause = onTogglePlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        headerActions = {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = PlumeStrings.get("queueTitle", currentLanguage),
                onClick = onOpenQueue,
                size = 40.dp,
                iconSize = 22.dp
            )
        },
        leadingControl = {
            GlassIconButton(
                icon = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                onClick = onToggleShuffle,
                active = playbackState.isShuffle,
                accent = ensureVisibleAccent(accent),
                size = 44.dp,
                iconSize = 22.dp,
                glass = false
            )
        },
        trailingControl = {
            GlassIconButton(
                icon = if (playbackState.repeatMode == RepeatMode.ONE) {
                    Icons.Default.RepeatOne
                } else {
                    Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                onClick = onToggleRepeat,
                active = playbackState.repeatMode != RepeatMode.OFF,
                accent = ensureVisibleAccent(accent),
                size = 44.dp,
                iconSize = 22.dp,
                glass = false
            )
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SoundCloud Full Player – works with Song model
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SoundCloudFullPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownloadToggle: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false
) {
    val accent = rememberDominantColor(song.thumbnailUrl)

    NowPlayingScaffold(
        artwork = song.thumbnailUrl.takeIf { it.isNotEmpty() },
        title = song.title,
        artist = song.artist,
        accent = accent,
        label = PlumeStrings.get("nowPlaying", currentLanguage),
        onDismiss = onDismiss,
        positionMs = positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
        isLoading = isLoading,
        isFavorite = isFavorite,
        isDownloaded = isDownloaded,
        isDownloading = isDownloading,
        favoriteLabel = PlumeStrings.get("favoriteShort", currentLanguage),
        playlistLabel = PlumeStrings.get("playlistShort", currentLanguage),
        downloadLabel = PlumeStrings.get("downloadShort", currentLanguage),
        downloadedLabel = PlumeStrings.get("savedShort", currentLanguage),
        onFavoriteToggle = onFavoriteToggle,
        onDownloadToggle = onDownloadToggle,
        onAddToPlaylist = onAddToPlaylist,
        onSeek = { fraction -> onSeekTo((fraction * durationMs).toLong()) },
        onTogglePlayPause = onTogglePlayPause,
        onNext = onNext,
        onPrevious = onPrevious
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Extract dominant color from artwork URL using Palette API
// ─────────────────────────────────────────────────────────────────────────────

/** In-memory accent cache so re-opening a player does not re-decode the artwork. */
private val dominantColorCache = HashMap<String, Color>()

@Composable
fun rememberDominantColor(artwork: Any?): Color {
    val fallback = Color(0xFF1A1A2E)
    var dominantColor by remember(artwork) {
        val cached = if (artwork is String) dominantColorCache[artwork] else null
        mutableStateOf(cached ?: fallback)
    }
    val context = LocalContext.current

    LaunchedEffect(artwork) {
        if (artwork == null) return@LaunchedEffect
        if (artwork is String) {
            if (artwork.isBlank()) return@LaunchedEffect
            if (dominantColorCache.containsKey(artwork)) {
                dominantColor = dominantColorCache[artwork] ?: fallback
                return@LaunchedEffect
            }
        }

        val rgb = withContext(Dispatchers.IO) {
            try {
                val bitmap = when (artwork) {
                    is Bitmap -> artwork
                    is String -> {
                        val request = ImageRequest.Builder(context)
                            .data(artwork)
                            .allowHardware(false)
                            .size(96, 96)
                            .build()
                        val result = context.imageLoader.execute(request)
                        (result as? SuccessResult)
                            ?.drawable
                            ?.let { it as? BitmapDrawable }
                            ?.bitmap
                    }
                    else -> null
                }
                bitmap?.let {
                    val palette = Palette.from(it).generate()
                    val swatch = palette.darkMutedSwatch
                        ?: palette.mutedSwatch
                        ?: palette.dominantSwatch
                        ?: palette.vibrantSwatch
                    swatch?.rgb
                }
            } catch (_: Exception) {
                null
            }
        }

        if (rgb != null) {
            val color = Color(rgb)
            if (artwork is String) {
                dominantColorCache[artwork] = color
            }
            dominantColor = color
        }
    }

    return dominantColor
}
