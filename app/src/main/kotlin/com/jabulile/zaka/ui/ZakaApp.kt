package com.jabulile.zaka.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jabulile.zaka.R
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.LoyaltyCardRepository
import com.jabulile.zaka.ui.components.MoneyText
import com.jabulile.zaka.ui.components.ZakaIcons
import com.jabulile.zaka.ui.expenses.ExpensesScreen
import com.jabulile.zaka.ui.expenses.ExpensesViewModel

/**
 * The four tabs, and the month-to-date total that sits above them wherever you
 * are.
 *
 * No navigation library. Four destinations and one modal form do not need a
 * back stack: the tab is `rememberSaveable` state, which survives rotation and
 * process death, and that is the whole requirement. Revisit this the moment
 * there are deep links or a third level of screen.
 */
enum class ZakaTab(val labelRes: Int, val icon: ImageVector) {
    EXPENSES(R.string.tab_expenses, ZakaIcons.Expenses),
    ADD(R.string.tab_add, ZakaIcons.Add),
    INSIGHTS(R.string.tab_insights, ZakaIcons.Insights),
    CARDS(R.string.tab_cards, ZakaIcons.Cards),
}

@Composable
fun ZakaApp(
    expenses: ExpenseRepository,
    cards: LoyaltyCardRepository,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(ZakaTab.EXPENSES) }

    val expensesViewModel: ExpensesViewModel =
        viewModel(factory = ExpensesViewModel.factory(expenses))
    val expensesState by expensesViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                MonthToDate(total = expensesState.monthTotal)
                NavigationBar {
                    ZakaTab.entries.forEach { candidate ->
                        val label = stringResource(candidate.labelRes)
                        NavigationBarItem(
                            selected = tab == candidate,
                            onClick = { tab = candidate },
                            icon = {
                                Icon(imageVector = candidate.icon, contentDescription = null)
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { insets ->
        when (tab) {
            ZakaTab.EXPENSES -> ExpensesScreen(
                state = expensesState,
                onEdit = { /* #6 */ },
                onDelete = expensesViewModel::delete,
                modifier = Modifier.padding(insets),
            )

            ZakaTab.ADD -> ComingSoon(R.string.tab_add, Modifier.padding(insets))
            ZakaTab.INSIGHTS -> ComingSoon(R.string.tab_insights, Modifier.padding(insets))
            ZakaTab.CARDS -> ComingSoon(R.string.tab_cards, Modifier.padding(insets))
        }
    }
}

/**
 * The strip the iOS app keeps above its tab bar. It is the one number worth
 * seeing from anywhere in the app, so it does not belong to any one screen.
 */
@Composable
private fun MonthToDate(total: Double, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.month_to_date)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clearAndSetSemantics {
                        contentDescription = "$label, ${Money.string(total)}"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(
                    amount = total,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ComingSoon(labelRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.coming_this_sprint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
