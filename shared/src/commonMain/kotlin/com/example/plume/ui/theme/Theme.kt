package com.example.plume.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = Black,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = PureWhite,
    secondary = GraySubtle,
    onSecondary = Black,
    secondaryContainer = DarkBorder,
    onSecondaryContainer = PureWhite,
    background = Black,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = GraySubtle,
    outline = BorderSubtle
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = PureWhite,
    primaryContainer = OffWhite,
    onPrimaryContainer = Black,
    secondary = GrayDark,
    onSecondary = PureWhite,
    secondaryContainer = OffWhite,
    onSecondaryContainer = Black,
    background = PureWhite,
    onBackground = Black,
    surface = PureWhite,
    onSurface = Black,
    surfaceVariant = OffWhite,
    onSurfaceVariant = GrayDark,
    outline = BorderSubtle
)

/**
 * Applies the platform-specific system bar appearance.
 *
 * Android draws edge-to-edge with transparent status/navigation bars, iOS relies
 * on the hosting `UIViewController` and therefore does nothing here.
 */
@Composable
expect fun PlatformSystemBars(darkTheme: Boolean)

@Composable
fun PlumeTheme(
    darkTheme: Boolean = true, // Force dark sleek monochrome by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme

    PlatformSystemBars(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
