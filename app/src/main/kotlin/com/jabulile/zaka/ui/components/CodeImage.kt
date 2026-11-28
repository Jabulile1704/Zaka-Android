package com.jabulile.zaka.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.core.CodeMatrix
import kotlin.math.floor

/**
 * Draws a code as rectangles, at whatever size it is given.
 *
 * **Always dark on light, never themed.** A scanner expects dark bars on a
 * light background and nothing else; inverting this for dark mode would
 * produce a code that looks right to a person and is unreadable to a till.
 *
 * Module edges are snapped to whole pixels. A barcode drawn on fractional
 * boundaries gets anti-aliased edges, which is exactly the blur that makes a
 * scaled bitmap barcode scan badly.
 */
@Composable
fun CodeImage(
    code: CodeMatrix,
    description: String,
    modifier: Modifier = Modifier,
    barHeight: Dp = 150.dp,
) {
    val surface = modifier
        .background(Color.White, RoundedCornerShape(12.dp))
        .padding(12.dp)
        .clearAndSetSemantics { contentDescription = description }

    when (code) {
        is CodeMatrix.Linear -> Box(surface) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
            ) {
                val total = code.width + code.quietZone * 2
                val module = floor(size.width / total).coerceAtLeast(1f)
                val drawn = module * total
                val left = (size.width - drawn) / 2f

                code.modules.forEachIndexed { index, dark ->
                    if (dark) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(left + (code.quietZone + index) * module, 0f),
                            size = Size(module, size.height),
                        )
                    }
                }
            }
        }

        is CodeMatrix.Grid -> Box(surface) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val total = code.width + code.quietZone * 2
                val module = floor(size.width / total).coerceAtLeast(1f)
                val drawn = module * total
                val origin = (size.width - drawn) / 2f + code.quietZone * module

                for (y in 0 until code.height) {
                    for (x in 0 until code.width) {
                        if (code[x, y]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(origin + x * module, origin + y * module),
                                size = Size(module, module),
                            )
                        }
                    }
                }
            }
        }
    }
}
