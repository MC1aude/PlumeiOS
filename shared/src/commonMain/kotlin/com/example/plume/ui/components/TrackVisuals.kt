package com.example.plume.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small "now playing" equalizer indicator shared by every track row.
 */
@Composable
fun PlayingEqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barHeights = if (isPlaying) listOf(14.dp, 22.dp, 10.dp) else listOf(8.dp, 12.dp, 8.dp)
        barHeights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Formats a duration given in seconds as `m:ss`.
 *
 * Uses `padStart` instead of `java.util.Locale`/`String.format` so the function
 * is safe to use from `commonMain`.
 */
fun formatDuration(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val mins = safeSeconds / 60
    val secs = safeSeconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

fun formatDurationMs(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    return formatDuration(totalSeconds)
}
