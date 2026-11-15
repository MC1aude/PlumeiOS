package com.example.plume.ui.theme

import androidx.compose.ui.graphics.Color

// ── Premium Monochrome & OLED Glass Palette (2026) ──
val Black = Color(0xFF000000)          // #000000 Deep OLED black background
val DarkSurface = Color(0xFF0D0D10)    // Premium dark surface
val DarkCard = Color(0xFF141418)       // Glassmorphism card base
val DarkCardElevated = Color(0xFF1C1C22) // Elevated surface
val DarkBorder = Color(0xFF262630)     // Subtle dividers
val BorderSubtle = Color(0x1AFFFFFF)   // 10% white border
val BorderGlow = Color(0x2BFFFFFF)     // 17% white glow border

val GrayDark = Color(0xFF3A3A3C)
val GrayMedium = Color(0xFF636366)
val GrayLight = Color(0xFF8E8E93)
val GraySubtle = Color(0xFFAEAEB2)

val OffWhite = Color(0xFFF2F2F7)
val PureWhite = Color(0xFFFFFFFF)

// Monochrome Theme Palette
val PrimaryMonochrome = PureWhite
val OnPrimaryMonochrome = Black
val SecondaryMonochrome = GraySubtle
val BackgroundMonochrome = Black
val SurfaceMonochrome = DarkSurface
val OnBackgroundMonochrome = PureWhite
val OnSurfaceMonochrome = PureWhite

// Ambient Neon Glow colors
val GlowPrimary = Color(0x40FFFFFF)    // Soft white ambient glow
val GlowSurface = Color(0x20FFFFFF)    // Subtle surface glow

// Glassmorphism tokens
val NowPlayingBase = Color(0xFF050508)
val GlassHighlight = Color(0x28FFFFFF)  // 16% white glass fill
val GlassBorderStrong = Color(0x66FFFFFF) // 40% white glass edge
val GlassBorderSoft = Color(0x18FFFFFF)
