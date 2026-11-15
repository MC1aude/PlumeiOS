package com.example.plume.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.plume.ui.theme.PlumeTheme

/**
 * Root composable exported by the shared module.
 *
 * Every platform (Android `MainActivity`, future iOS `ComposeUIViewController`)
 * renders its navigation host inside this function so theming and the edge-to-edge
 * background stay consistent across platforms.
 */
@Composable
fun PlumeRoot(
    darkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PlumeTheme(darkTheme = darkTheme) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
