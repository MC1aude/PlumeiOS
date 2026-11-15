package com.example.plume.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.plume.ui.screens.search.SoundCloudPlayerViewModel

/**
 * Floating glass player for SoundCloud streaming tracks.
 * Features dark charcoal glass base (92% opacity) for high contrast and legibility,
 * top glowing progress rail, and tactile spring press feedback.
 */
@Composable
fun SoundCloudMiniPlayer(
    viewModel: SoundCloudPlayerViewModel,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val position by viewModel.positionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()

    val song = currentSong ?: return

    val progress by remember(position, duration) {
        derivedStateOf {
            if (duration > 0) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val accent = ensureVisibleAccent(rememberDominantColor(song.thumbnailUrl))
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "soundCloudMiniPlayerScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = accent.copy(alpha = 0.35f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onOpenFullPlayer
            ),
        color = Color(0xFF0C0C10).copy(alpha = 0.92f), // Opaque dark charcoal base
        shape = shape,
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
        ) {
            // Glowing hairline progress rail
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            ) {
                drawRect(Color.White.copy(alpha = 0.12f))
                if (progress > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.65f), accent)
                        ),
                        size = Size(size.width * progress, size.height)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist (Crisp white readability)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls: Prev | Play/Pause | Next
                GlassIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = { viewModel.playPrevious() },
                    size = 36.dp,
                    iconSize = 18.dp,
                    glass = false
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(horizontal = 4.dp),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    GlassIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = { viewModel.togglePlayPause() },
                        size = 38.dp,
                        iconSize = 20.dp,
                        glass = false
                    )
                }

                GlassIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    onClick = { viewModel.playNext() },
                    size = 36.dp,
                    iconSize = 18.dp,
                    glass = false
                )
            }
        }
    }
}
