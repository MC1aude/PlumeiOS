package com.example.plume.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Minimal Compose Multiplatform entry point proving that the common Compose
 * dependencies are wired correctly. The Android app can consume it as-is.
 */
@Composable
fun SharedGreeting(
    title: String = "Plume Shared",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = currentPlatform().name,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
