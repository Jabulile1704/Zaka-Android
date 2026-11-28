package com.jabulile.zaka.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive, not a Liquid Glass impersonation.
 *
 * The iOS app is built on glass that refracts what is behind it; Android has
 * no such material and faking it is the clearest sign of a port. So the
 * information architecture carries over and the surface does not: expressive
 * colour, shape and motion, and the wallpaper's own palette where the device
 * offers one.
 */
@Composable
fun ZakaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> ZakaDarkColors
        else -> ZakaLightColors
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ZakaShapes,
        typography = ZakaTypography,
        content = content,
    )
}
