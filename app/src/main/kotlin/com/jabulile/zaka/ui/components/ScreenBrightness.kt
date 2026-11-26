package com.jabulile.zaka.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Full brightness, and no screen timeout, for as long as this is composed.
 *
 * Till scanners struggle with a dim screen, and a card held up at a counter is
 * exactly the moment the display would otherwise decide to sleep. Both are put
 * back on the way out, so the phone's own brightness is only borrowed.
 */
@Composable
fun KeepScreenBrightFor() {
    val context = LocalContext.current

    DisposableEffect(context) {
        val window = context.findActivity()?.window
        val previous = window?.attributes?.screenBrightness

        window?.let {
            it.attributes = it.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.let {
                it.attributes = it.attributes.apply {
                    screenBrightness = previous
                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

/** Compose hands out a context that may be wrapped several times over. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
