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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import com.example.plume.data.repository.rememberTrackArtwork
import com.example.plume.domain.model.PlaybackState

/**
 * Floating glass player for local & library track playback.
 * Features a high-opacity dark charcoal glass surface for 100% text legibility,
 * glowing hairline progress rail, and tactile spring press feedback.
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val artwork = rememberTrackArtwork(track)
    val accent = ensureVisibleAccent(rememberDominantColor(artwork))

    val progress by remember(playbackState.positionMs, playbackState.durationMs) {
        derivedStateOf {
            if (playbackState.durationMs > 0) {
                (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "miniPlayerCardScale"
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
                onClick = onClick
            ),
        color = Color(0xFF0C0C10).copy(alpha = 0.92f), // High opacity charcoal base prevents text/artwork bleed
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
                // Artwork
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        AsyncImage(
                            model = artwork,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF282834), Color(0xFF16161E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.50f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata (100% legible crisp white text)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tactile Controls
                if (playbackState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    GlassIconButton(
                        icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        onClick = onTogglePlayPause,
                        size = 38.dp,
                        iconSize = 20.dp,
                        glass = false
                    )
                }

                GlassIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    onClick = onNext,
                    size = 38.dp,
                    iconSize = 20.dp,
                    glass = false
                )
            }
        }
    }
}
