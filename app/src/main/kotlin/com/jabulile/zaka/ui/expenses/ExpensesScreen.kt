package com.jabulile.zaka.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.DayGroup
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.ui.components.CategoryMark
import com.jabulile.zaka.ui.components.MoneyText
import com.jabulile.zaka.ui.components.ZakaIcons
import java.time.LocalDate

/**
 * Everything logged, grouped by day, newest first. Tap a row to edit it, swipe
 * it away to delete it.
 */
@Composable
fun ExpensesScreen(
    state: ExpensesUiState,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loaded && state.days.isEmpty()) {
        EmptyExpenses(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        state.days.forEach { day ->
            item(key = "day-${day.day}") {
                DayHeading(day = day, today = state.today)
            }
            items(items = day.expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    onEdit = { onEdit(expense.id) },
                    onDelete = { onDelete(expense.id) },
                )
            }
        }
    }
}

@Composable
private fun DayHeading(
    day: DayGroup,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val title = day.title(today)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // One announcement for the heading, not two unrelated fragments.
            .clearAndSetSemantics {
                heading()
                contentDescription = "$title, ${Money.string(day.total)}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        MoneyText(
            amount = day.total,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipe = rememberSwipeToDismissBoxState()
    val deleteLabel = stringResource(R.string.expense_delete_action)

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        // One direction only. A row that deletes whichever way it is pushed is
        // too easy to lose an expense to.
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) onDelete()
        },
        backgroundContent = { DeleteBackground() },
    ) {
        ListItem(
            modifier = Modifier
                .clickable(
                    onClickLabel = stringResource(R.string.expense_edit_action),
                    onClick = onEdit,
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(deleteLabel) {
                            onDelete()
                            true
                        },
                    )
                },
            headlineContent = { Text(expense.title) },
            supportingContent = { Text(expense.category.label) },
            leadingContent = { CategoryMark(category = expense.category) },
            trailingContent = {
                MoneyText(
                    amount = expense.amount,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

@Composable
private fun DeleteBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = ZakaIcons.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.expenses_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.expenses_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
