package com.example.plume.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.Song
import com.example.plume.ui.components.SongTrackItem

@Composable
fun SearchScreen(
    uiState: SoundCloudSearchState,
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.RUSSIAN,
    onQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header Bar (Fixed status bar padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = PlumeStrings.get("search", currentLanguage).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize = 22.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Glass Search TextField
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        PlumeStrings.get("searchPlaceholder", currentLanguage),
                        color = Color.White.copy(alpha = 0.45f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color(0xFF14141A).copy(alpha = 0.90f),
                    unfocusedContainerColor = Color(0xFF101014).copy(alpha = 0.70f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Search Results Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                uiState.query.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PlumeStrings.get("emptySearch", currentLanguage),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.songs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PlumeStrings.get("notFound", currentLanguage),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp)
                    ) {
                        items(
                            items = uiState.songs,
                            key = { it.id },
                            contentType = { "search_song" }
                        ) { song ->
                            val isCurrent = currentPlayingSong?.id == song.id
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                SongTrackItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    onClick = { onSongClick(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
