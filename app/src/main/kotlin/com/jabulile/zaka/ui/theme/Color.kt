package com.jabulile.zaka.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Zaka's own palette, used when the device has no dynamic colour to offer.
 *
 * The app's mark is a two-tone rand coin, so primary is its brass and
 * secondary its nickel; tertiary is the green that reads as money here. The
 * surfaces are warm rather than neutral grey, which keeps a screen full of
 * numbers from looking like a spreadsheet.
 */

private val Brass40 = Color(0xFF7A5A12)
private val Brass30 = Color(0xFF5C4300)
private val Brass80 = Color(0xFFF0C04E)
private val Brass90 = Color(0xFFFFDF9C)
private val Brass20 = Color(0xFF3F2E00)

private val Nickel40 = Color(0xFF5A5E63)
private val Nickel30 = Color(0xFF434750)
private val Nickel80 = Color(0xFFC2C7CE)
private val Nickel90 = Color(0xFFDEE2E8)
private val Nickel20 = Color(0xFF2C3036)

private val Green40 = Color(0xFF10684F)
private val Green30 = Color(0xFF00513B)
private val Green80 = Color(0xFF88D6B8)
private val Green90 = Color(0xFFA4F2D3)
private val Green20 = Color(0xFF003828)

internal val ZakaLightColors = lightColorScheme(
    primary = Brass40,
    onPrimary = Color.White,
    primaryContainer = Brass90,
    onPrimaryContainer = Brass30,
    inversePrimary = Brass80,
    secondary = Nickel40,
    onSecondary = Color.White,
    secondaryContainer = Nickel90,
    onSecondaryContainer = Nickel30,
    tertiary = Green40,
    onTertiary = Color.White,
    tertiaryContainer = Green90,
    onTertiaryContainer = Green30,
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1D1B16),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceDim = Color(0xFFDDD8CE),
    surfaceBright = Color(0xFFFBF8F3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F1E8),
    surfaceContainer = Color(0xFFF0EBE1),
    surfaceContainerHigh = Color(0xFFEAE5DB),
    surfaceContainerHighest = Color(0xFFE4DFD5),
    inverseSurface = Color(0xFF32302A),
    inverseOnSurface = Color(0xFFF5F0E6),
    outline = Color(0xFF7E7667),
    outlineVariant = Color(0xFFCFC6B4),
    scrim = Color.Black,
)

internal val ZakaDarkColors = darkColorScheme(
    primary = Brass80,
    onPrimary = Brass20,
    primaryContainer = Brass30,
    onPrimaryContainer = Brass90,
    inversePrimary = Brass40,
    secondary = Nickel80,
    onSecondary = Nickel20,
    secondaryContainer = Nickel30,
    onSecondaryContainer = Nickel90,
    tertiary = Green80,
    onTertiary = Green20,
    tertiaryContainer = Green30,
    onTertiaryContainer = Green90,
    background = Color(0xFF14110D),
    onBackground = Color(0xFFE8E2D9),
    surface = Color(0xFF14110D),
    onSurface = Color(0xFFE8E2D9),
    surfaceVariant = Color(0xFF4C4639),
    onSurfaceVariant = Color(0xFFCFC6B4),
    surfaceDim = Color(0xFF14110D),
    surfaceBright = Color(0xFF3B362E),
    surfaceContainerLowest = Color(0xFF0E0C09),
    surfaceContainerLow = Color(0xFF1C1915),
    surfaceContainer = Color(0xFF201D18),
    surfaceContainerHigh = Color(0xFF2B2722),
    surfaceContainerHighest = Color(0xFF363129),
    inverseSurface = Color(0xFFE8E2D9),
    inverseOnSurface = Color(0xFF32302A),
    outline = Color(0xFF988F7E),
    outlineVariant = Color(0xFF4C4639),
    scrim = Color.Black,
)
