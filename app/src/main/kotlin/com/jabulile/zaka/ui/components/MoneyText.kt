package com.jabulile.zaka.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.ui.theme.TabularFigures

/**
 * An amount in rand, with tabular figures.
 *
 * Proportional digits make a total jump as it changes, which it does on every
 * keystroke in the add form and on every delete in the list.
 */
@Composable
fun MoneyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    rounded: Boolean = false,
) {
    Text(
        text = if (rounded) Money.rounded(amount) else Money.string(amount),
        modifier = modifier,
        style = style.merge(TabularFigures),
        color = color,
    )
}
