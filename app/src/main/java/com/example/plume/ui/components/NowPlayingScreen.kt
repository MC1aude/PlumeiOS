package com.example.plume.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.plume.ui.theme.NowPlayingBase
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Now Playing: immersive glassmorphism backdrop + controls
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full screen (edge-to-edge) Now Playing surface.
 *
 * Guaranteed 100% click responsiveness on all action buttons and transport controls
 * with zero gesture interference.
 */
@Composable
fun NowPlayingScaffold(
    artwork: Any?,
    title: String,
    artist: String,
    accent: Color,
    label: String,
    onDismiss: () -> Unit,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    onSeek: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    headerActions: @Composable RowScope.() -> Unit = {},
    actionRow: (@Composable () -> Unit)? = null,
    isFavorite: Boolean = false,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    favoriteLabel: String = "Favorite",
    playlistLabel: String = "Playlist",
    downloadLabel: String = "Download",
    downloadedLabel: String = "Saved",
    onFavoriteToggle: (() -> Unit)? = null,
    onDownloadToggle: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    leadingControl: (@Composable () -> Unit)? = null,
    trailingControl: (@Composable () -> Unit)? = null,
) {
    val accentColor = remember(accent) { ensureVisibleAccent(accent) }

    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var pendingSeekFraction by remember { mutableFloatStateOf(-1f) }

    val totalMs = durationMs.coerceAtLeast(1L)
    val displayFraction by remember(scrubbing, scrubFraction, pendingSeekFraction, positionMs, totalMs) {
        derivedStateOf {
            when {
                scrubbing -> scrubFraction
                pendingSeekFraction >= 0f -> pendingSeekFraction
                else -> (positionMs.toFloat() / totalMs).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(pendingSeekFraction, positionMs) {
        if (pendingSeekFraction >= 0f &&
            abs(positionMs.toFloat() / totalMs - pendingSeekFraction) < 0.05f
        ) {
            pendingSeekFraction = -1f
        }
    }
    LaunchedEffect(pendingSeekFraction) {
        if (pendingSeekFraction >= 0f) {
            delay(1500L)
            pendingSeekFraction = -1f
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePadding = if (maxWidth < 360.dp) 18.dp else 24.dp
        val compact = maxHeight < 620.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            NowPlayingBackdrop(artwork = artwork, accent = accentColor)

            // Balanced vertical layout using Arrangement.SpaceBetween to fill full viewport height without dead void
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        start = sidePadding,
                        end = sidePadding,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Drag handle / Dismiss indicator
                Box(
                    Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f))
                        .clickable { onDismiss() }
                )

                // Header
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = label,
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart),
                        size = 40.dp,
                        iconSize = 24.dp
                    )

                    Text(
                        text = label.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            fontSize = 10.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.68f)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = headerActions
                    )
                }

                // Artwork (proportional, responsive sizing)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = 100.dp, max = 340.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val available = minOf(maxWidth, maxHeight).coerceAtMost(340.dp)
                    if (available >= 72.dp) {
                        NowPlayingArtwork(
                            artwork = artwork,
                            accent = accentColor,
                            size = available
                        )
                    }
                }

                // Title + artist
                Crossfade(
                    targetState = title to artist,
                    animationSpec = tween(280),
                    label = "metadata"
                ) { (trackTitle, trackArtist) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = trackTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = trackArtist,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 13.5.sp,
                                letterSpacing = 0.1.sp
                            ),
                            color = Color.White.copy(alpha = 0.62f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Explicit Horizontal Action Row (Guaranteed click responsiveness) ──
                val effectiveActionRow = actionRow ?: {
                    NowPlayingActionRow(
                        isFavorite = isFavorite,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        accent = accentColor,
                        favoriteLabel = favoriteLabel,
                        playlistLabel = playlistLabel,
                        downloadLabel = downloadLabel,
                        downloadedLabel = downloadedLabel,
                        onFavoriteClick = { onFavoriteToggle?.invoke() },
                        onPlaylistClick = { onAddToPlaylist?.invoke() },
                        onDownloadClick = { onDownloadToggle?.invoke() }
                    )
                }

                effectiveActionRow()

                // Seek bar + timers
                Column(Modifier.fillMaxWidth()) {
                    GlowingSeekBar(
                        progress = displayFraction,
                        accent = accentColor,
                        enabled = durationMs > 0,
                        onProgressChange = { fraction ->
                            scrubbing = true
                            scrubFraction = fraction
                        },
                        onSeekFinished = { fraction ->
                            scrubbing = false
                            scrubFraction = fraction
                            pendingSeekFraction = fraction
                            onSeek(fraction)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeLabel(formatDurationMs((displayFraction * totalMs).toLong()))
                        TimeLabel(formatDurationMs(totalMs))
                    }
                }

                // Transport controls (ergonomic thumb-zone placement at bottom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingControl != null) {
                        leadingControl()
                    } else {
                        Spacer(Modifier.size(44.dp))
                    }

                    GlassIconButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        onClick = onPrevious,
                        size = if (compact) 44.dp else 48.dp,
                        iconSize = if (compact) 24.dp else 26.dp,
                        glass = false
                    )

                    GlowPlayButton(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        accent = accentColor,
                        onClick = onTogglePlayPause,
                        compact = compact
                    )

                    GlassIconButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        onClick = onNext,
                        size = if (compact) 44.dp else 48.dp,
                        iconSize = if (compact) 24.dp else 26.dp,
                        glass = false
                    )

                    if (trailingControl != null) {
                        trailingControl()
                    } else {
                        Spacer(Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom Monochrome Black & White Glassmorphism Toast Banner
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sleek, floating minimalist monochrome black & white notification banner rendered at the top
 * of the screen (below the status bar) with pure white text, silver/white icon tint, and
 * a subtle white-to-transparent border glow.
 */
@Composable
fun PlumeToastBanner(
    message: String?,
    icon: ImageVector = Icons.Default.Favorite,
    accent: Color = Color.White,
    onDismiss: () -> Unit = {}
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = message != null,
        enter = androidx.compose.animation.slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { -it * 2 }
        ) + androidx.compose.animation.fadeIn(animationSpec = tween(200)),
        exit = androidx.compose.animation.slideOutVertically(
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            targetOffsetY = { -it * 2 }
        ) + androidx.compose.animation.fadeOut(animationSpec = tween(150)),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .zIndex(999f)
    ) {
        if (message != null) {
            LaunchedEffect(message) {
                delay(2500L)
                onDismiss()
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.White.copy(alpha = 0.15f)
                    ),
                color = Color(0xFF111115).copy(alpha = 0.90f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.01f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Artwork
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingArtwork(
    artwork: Any?,
    accent: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = 26.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black,
                spotColor = accent.copy(alpha = 0.50f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Crossfade(
            targetState = artwork,
            animationSpec = tween(450),
            label = "artwork"
        ) { model ->
            if (model == null || (model is String && model.isEmpty())) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF282834),
                                    Color(0xFF14141A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.10f),
                        0.3f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.28f)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Backdrop
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NowPlayingBackdrop(
    artwork: Any?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(NowPlayingBase)) {
        if (artwork != null && !(artwork is String && artwork.isEmpty())) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.85f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.25f
                    }
                    .blur(radius = 80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.10f),
                    radius = size.minDimension * 0.85f
                ),
                radius = size.minDimension * 0.85f,
                center = Offset(size.width * 0.82f, size.height * 0.10f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.42f),
                    radius = size.minDimension * 0.70f
                ),
                radius = size.minDimension * 0.70f,
                center = Offset(size.width * 0.10f, size.height * 0.42f)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.22f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent)
                    )
                )
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.20f),
                        0.22f to Color.Black.copy(alpha = 0.30f),
                        0.50f to Color.Black.copy(alpha = 0.66f),
                        0.78f to Color.Black.copy(alpha = 0.88f),
                        1f to Color.Black.copy(alpha = 0.96f)
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Buttons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    glass: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (glass) {
                    Modifier
                        .clip(CircleShape)
                        .background(
                            if (active) accent.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.09f)
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            CircleShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) accent else Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GlowPlayButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val transition = rememberInfiniteTransition(label = "playGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playGlowPulse"
    )
    val haloAlpha = if (isPlaying) pulse else 0.42f

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playButtonScale"
    )

    Box(
        modifier = modifier.size(if (compact) 64.dp else 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.50f * haloAlpha),
                        accent.copy(alpha = 0.16f * haloAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 2f
                ),
                radius = size.minDimension / 2f,
                center = center
            )
        }

        Box(
            modifier = Modifier
                .size(if (compact) 54.dp else 68.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = accent.copy(alpha = 0.55f),
                    spotColor = accent.copy(alpha = 0.75f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFE4E4EC))
                    )
                )
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = Color.Black),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                    color = Color.Black,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(if (compact) 30.dp else 36.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Horizontal Action Row (3 Evenly Spaced Glassmorphic Buttons)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NowPlayingActionRow(
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    accent: Color,
    favoriteLabel: String = "Favorite",
    playlistLabel: String = "Playlist",
    downloadLabel: String = "Download",
    downloadedLabel: String = "Saved",
    onFavoriteClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Favorite / Like Button with Heart Pop Animation
        FavoritePill(
            isFavorite = isFavorite,
            accent = accent,
            label = favoriteLabel,
            onClick = onFavoriteClick,
            modifier = Modifier.weight(1f)
        )

        // 2. Add to Playlist Button
        NowPlayingActionPill(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            label = playlistLabel,
            onClick = onPlaylistClick,
            active = false,
            accent = accent,
            modifier = Modifier.weight(1f)
        )

        // 3. Download / Save Button
        NowPlayingActionPill(
            icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
            label = if (isDownloaded) downloadedLabel else downloadLabel,
            onClick = onDownloadClick,
            active = isDownloaded,
            accent = accent,
            loading = isDownloading,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FavoritePill(
    isFavorite: Boolean,
    accent: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            bounce.animateTo(
                targetValue = 1.40f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            bounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    val heartColor = if (isFavorite) Color.White else Color.White.copy(alpha = 0.88f)

    NowPlayingActionPill(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        label = label,
        onClick = onClick,
        modifier = modifier,
        active = isFavorite,
        accent = heartColor,
        iconScale = bounce.value
    )
}

@Composable
fun NowPlayingActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Color = Color.White,
    loading: Boolean = false,
    iconScale: Float = 1f
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionPillPressScale"
    )
    val pillShape = RoundedCornerShape(22.dp)

    Surface(
        modifier = modifier
            .height(40.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (active) 12.dp else 4.dp,
                shape = pillShape,
                ambientColor = Color.Black,
                spotColor = if (active) accent.copy(alpha = 0.5f) else Color.Transparent
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        color = if (active) accent.copy(alpha = 0.20f) else Color(0xFF141418).copy(alpha = 0.75f),
        shape = pillShape,
        border = BorderStroke(
            1.dp,
            if (active) accent.copy(alpha = 0.50f)
            else Color.White.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) accent else Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp
                ),
                color = if (active) accent else Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Seek bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlowingSeekBar(
    progress: Float,
    accent: Color,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (dragging) {
            androidx.compose.animation.core.snap()
        } else {
            tween(durationMillis = 450, easing = LinearEasing)
        },
        label = "seekProgress"
    )
    val fraction = if (dragging) dragFraction else animatedProgress
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "seekThumbScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInputEnabled(enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(dragFraction)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(dragFraction)
                    },
                    onDragEnd = {
                        dragging = false
                        onSeekFinished(dragFraction)
                    },
                    onDragCancel = {
                        dragging = false
                    }
                )
            }
            .pointerInputEnabled(enabled) {
                detectTapGestures(
                    onTap = { offset ->
                        val target = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekFinished(target)
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackHeight = 4.dp.toPx()
            val thumbX = size.width * fraction
            val thumbRadius = 6.dp.toPx() * thumbScale
            val glowRadius = 24.dp.toPx() * thumbScale

            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            if (thumbX > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.70f),
                            accent.copy(alpha = 0.95f),
                            Color.White
                        ),
                        startX = 0f,
                        endX = thumbX.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(thumbX, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(thumbX, centerY),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(thumbX, centerY)
            )

            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = thumbRadius * 0.45f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        ),
        color = Color.White.copy(alpha = 0.55f)
    )
}

fun ensureVisibleAccent(color: Color): Color {
    return if (color.alpha < 0.5f || color.luminance() < 0.16f) {
        Color(0xFFEDEDF3)
    } else {
        color
    }
}

private fun Modifier.pointerInputEnabled(
    enabled: Boolean,
    block: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit
): Modifier {
    return if (enabled) {
        this.then(Modifier.pointerInput(enabled, block))
    } else {
        this
    }
}
