package com.jabulile.zaka.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Material's own type scale, unchanged.
 *
 * The iOS app leans on Dynamic Type and SF; the Android app leans on the
 * platform's type scale and its system font. Inventing a third scale would
 * only mean worse metrics and a font to ship.
 */
internal val ZakaTypography = Typography()

/**
 * Figures that do not jiggle. Merge this into any text showing an amount: the
 * month total updates as you type, and proportional digits make it jump.
 */
internal val TabularFigures = TextStyle(fontFeatureSettings = "tnum")
