package com.example.plume.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.ui.theme.BorderGlow

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onClearCache: () -> Unit,
    onClearDownloads: () -> Unit,
    onClearMessage: () -> Unit
) {
    var langMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lang = uiState.language

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp)
        ) {
            // Top Header (Fixed status bar padding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = PlumeStrings.get("settings", lang).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 22.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101014)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderGlow)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.01f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = PlumeStrings.get("appLanguage", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { langMenuExpanded = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.language.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = PlumeStrings.get("change", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false },
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF14141A))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        ) {
                            AppLanguage.entries.forEach { l ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = l.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        langMenuExpanded = false
                                        onSelectLanguage(l)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Cache Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101014)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderGlow)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.01f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = PlumeStrings.get("cacheAndStorage", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cache Clear Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PlumeStrings.get("audioCache", lang),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = uiState.cacheSize,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }

                        Button(
                            onClick = onClearCache,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(PlumeStrings.get("clear", lang))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Downloads Clear Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PlumeStrings.get("offlineTracks", lang),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = uiState.downloadedSize,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }

                        Button(
                            onClick = onClearDownloads,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(PlumeStrings.get("deleteAll", lang))
                        }
                    }
                }
            }

            // About Application Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101014)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderGlow)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.01f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = PlumeStrings.get("aboutApp", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(PlumeStrings.get("version", lang), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f))
                        Text("1.0.0 (Build 1)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(PlumeStrings.get("catalogSource", lang), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f))
                        Text("SoundCloud API v2", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(PlumeStrings.get("uiTheme", lang), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(PlumeStrings.get("darkMonochrome", lang), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }
}
