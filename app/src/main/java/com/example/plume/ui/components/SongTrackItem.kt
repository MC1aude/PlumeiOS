package com.example.plume.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.plume.domain.model.Song
import com.example.plume.ui.theme.BorderGlow

@Composable
fun SongTrackItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
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

            Spacer(modifier = Modifier.width(16.dp))

            // Title & Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
