package com.example.plume.ui.screens.home

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.Song
import com.example.plume.ui.components.PlayingEqualizerBars
import com.example.plume.ui.theme.BorderGlow

@Composable
fun HomeScreen(
    uiState: HomeSoundCloudUiState,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onFilterSelected: (String) -> Unit = {},
    onSongClick: (Song) -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(PlumeStrings.get("clear", currentLanguage))
                        }
                    }
                }
            }
            uiState.songs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PlumeStrings.get("notFound", currentLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    // ── Sleek Branded Plume Header & Recommendation Status ──
                    item(key = "home_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.20f),
                                                        Color.White.copy(alpha = 0.05f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "PLUME",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 24.sp,
                                                letterSpacing = 2.sp
                                            ),
                                            color = Color.White
                                        )
                                        Text(
                                            text = uiState.recommendationBadge,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 10.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                // Ambient recommendation indicator pill
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, BorderGlow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SMART FLOW",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic recommendation filter chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 12.dp)
                            ) {
                                items(
                                    items = uiState.availableFilters,
                                    key = { "filter_$it" }
                                ) { filter ->
                                    val selected = uiState.selectedFilter == filter
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (selected) Color.White
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) Color.White else BorderGlow,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .clickable { onFilterSelected(filter) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.5.sp
                                            ),
                                            color = if (selected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Quick Picks Grid (2 columns) ──
                    if (uiState.quickPicks.isNotEmpty()) {
                        item(key = "quick_picks_section") {
                            Spacer(modifier = Modifier.height(12.dp))
                            QuickPicksGrid(
                                songs = uiState.quickPicks,
                                currentPlayingSong = currentPlayingSong,
                                isPlaying = isPlaying,
                                onSongClick = onSongClick
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    // ── Category Carousels ──
                    items(
                        items = uiState.categories,
                        key = { "category_${it.title}" },
                        contentType = { "category" }
                    ) { category ->
                        CategoryCarousel(
                            title = category.title,
                            songs = category.songs,
                            currentPlayingSong = currentPlayingSong,
                            isPlaying = isPlaying,
                            onSongClick = onSongClick
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Picks Grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickPicksGrid(
    songs: List<Song>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        songs.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { song ->
                    val isCurrent = currentPlayingSong?.id == song.id
                    QuickPickCard(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onClick = { onSongClick(song) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(cardShape)
            .clickable { onClick() },
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else Color(0xFF141418).copy(alpha = 0.80f),
        shape = cardShape,
        border = BorderStroke(
            1.dp,
            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else BorderGlow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isCurrent) 0.08f else 0.04f),
                            Color.White.copy(alpha = if (isCurrent) 0.02f else 0.01f)
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
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

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Carousel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryCarousel(
    title: String,
    songs: List<Song>,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = { "song_carousel" }
            ) { song ->
                val isCurrent = currentPlayingSong?.id == song.id
                CarouselCard(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun CarouselCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val artworkShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(152.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = 12.dp,
                    shape = artworkShape,
                    ambientColor = Color.Black,
                    spotColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.Black
                )
                .clip(artworkShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else BorderGlow,
                    artworkShape
                ),
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
                    modifier = Modifier.size(36.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                        )
                    )
            )

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    PlayingEqualizerBars(
                        isPlaying = isPlaying,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
