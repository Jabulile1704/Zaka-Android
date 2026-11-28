package com.jabulile.zaka.ui.theme

import androidx.compose.ui.graphics.Color
import com.jabulile.zaka.core.ExpenseCategory

/**
 * The category's colour as Compose sees it.
 *
 * The core module keeps these as hex triples so it stays free of Android; this
 * is the one place that turns them into a [Color].
 */
val ExpenseCategory.color: Color
    get() = Color(colorHex.toLong(radix = 16) or 0xFF000000L)
