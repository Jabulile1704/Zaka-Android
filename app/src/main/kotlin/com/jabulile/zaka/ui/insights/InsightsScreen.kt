package com.jabulile.zaka.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.core.SpendBreakdown
import com.jabulile.zaka.core.SpendSlice
import com.jabulile.zaka.data.BreakdownStyle
import com.jabulile.zaka.ui.components.CategoryMark
import com.jabulile.zaka.ui.components.MoneyText
import com.jabulile.zaka.ui.components.ZakaIcons
import com.jabulile.zaka.ui.theme.color
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The month's total, what that is per day, and where the money went. The
 * arrows step back through months; the choice of donut or bars is remembered.
 */
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStyle: (BreakdownStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MonthStepper(
            month = state.month,
            canGoForward = state.canGoForward,
            onPrevious = onPrevious,
            onNext = onNext,
        )

        Totals(total = state.total, perDay = state.perDay)

        if (state.loaded && state.slices.isEmpty()) {
            Text(
                text = stringResource(R.string.insights_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        StylePicker(style = state.style, onStyle = onStyle)

        when (state.style) {
            BreakdownStyle.DONUT -> {
                SpendDonut(slices = state.slices, total = state.total)
                Legend(slices = state.slices, total = state.total)
            }

            BreakdownStyle.BARS -> SpendBars(slices = state.slices, total = state.total)
        }
    }
}

@Composable
private fun MonthStepper(
    month: YearMonth,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = ZakaIcons.Previous,
                contentDescription = stringResource(R.string.insights_previous_month),
            )
        }
        Text(
            text = month.format(formatter),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                imageVector = ZakaIcons.Next,
                contentDescription = stringResource(R.string.insights_next_month),
            )
        }
    }
}

@Composable
private fun Totals(total: Double, perDay: Double, modifier: Modifier = Modifier) {
    val totalLabel = stringResource(R.string.insights_total)
    val perDayLabel = stringResource(R.string.insights_per_day)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription =
                    "$totalLabel ${Money.string(total)}, $perDayLabel ${Money.string(perDay)}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MoneyText(
            amount = total,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = "$perDayLabel ${Money.string(perDay)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StylePicker(
    style: BreakdownStyle,
    onStyle: (BreakdownStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = BreakdownStyle.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = style == option,
                onClick = { onStyle(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        stringResource(
                            when (option) {
                                BreakdownStyle.DONUT -> R.string.insights_donut
                                BreakdownStyle.BARS -> R.string.insights_bars
                            },
                        ),
                    )
                },
            )
        }
    }
}

/**
 * The reveal sweep comes from [SpendBreakdown.sweep] in `:core`, so what the
 * chart draws part-way through an animation is the same tested arithmetic that
 * keeps the circle whole at every step.
 */
@Composable
private fun SpendDonut(
    slices: List<SpendSlice>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    val key = slices.joinToString { it.id }
    var target by remember(key) { mutableFloatStateOf(0f) }
    LaunchedEffect(key) { target = 1f }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "donut reveal",
    )

    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val mergedColor = MaterialTheme.colorScheme.outline
    val colors = slices.map { it.category?.color ?: mergedColor }
    val sweep = SpendBreakdown.sweep(slices, progress.toDouble())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thickness = 32.dp.toPx()
            val diameter = min(size.width, size.height) - thickness
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )

            drawArc(
                color = empty,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = thickness),
            )

            if (total <= 0) return@Canvas

            var start = -90f
            sweep.drawn.forEachIndexed { index, drawn ->
                val degrees = (drawn / total * 360.0).toFloat()
                drawArc(
                    color = colors[index],
                    startAngle = start,
                    sweepAngle = degrees,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = thickness),
                )
                start += degrees
            }
        }
        MoneyText(
            amount = total,
            style = MaterialTheme.typography.titleLarge,
            rounded = true,
        )
    }
}

@Composable
private fun Legend(
    slices: List<SpendSlice>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = slice.announcement(total)
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SliceMark(slice = slice)
                Text(text = slice.label, modifier = Modifier.fillMaxWidth(0.5f))
                Text(
                    text = slice.percentage(total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(amount = slice.amount, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SpendBars(
    slices: List<SpendSlice>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val mergedColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        slices.forEach { slice ->
            val share = slice.share(total).toFloat()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = slice.announcement(total)
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SliceMark(slice = slice)
                    Text(text = slice.label, modifier = Modifier.fillMaxWidth(0.5f))
                    Text(
                        text = slice.percentage(total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyText(amount = slice.amount, style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(trackColor, RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share.coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(
                                slice.category?.color ?: mergedColor,
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SliceMark(slice: SpendSlice, modifier: Modifier = Modifier) {
    val category = slice.category
    if (category != null) {
        CategoryMark(category = category, modifier = modifier, size = 28.dp)
    } else {
        Box(
            modifier = modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

private fun SpendSlice.percentage(total: Double): String =
    "${(share(total) * 100).roundToInt()}%"

private fun SpendSlice.announcement(total: Double): String =
    "$label, ${percentage(total)}, ${Money.string(amount)}"
