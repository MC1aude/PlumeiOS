package com.example.plume.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun PlatformSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                // Draw edge-to-edge and keep the system bars fully transparent so the
                // Now Playing artwork/backdrop bleeds behind status & nav bars seamlessly.
                WindowCompat.setDecorFitsSystemWindows(window, false)
                @Suppress("DEPRECATION")
                window.statusBarColor = AndroidColor.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = AndroidColor.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
}
