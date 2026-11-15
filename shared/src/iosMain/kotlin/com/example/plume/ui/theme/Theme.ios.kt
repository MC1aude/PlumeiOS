package com.example.plume.ui.theme

import androidx.compose.runtime.Composable

/**
 * iOS system bar appearance is controlled by the hosting UIViewController
 * (`preferredStatusBarStyle`), so the shared theme does not touch it.
 */
@Composable
actual fun PlatformSystemBars(darkTheme: Boolean) {
    // No-op
}
