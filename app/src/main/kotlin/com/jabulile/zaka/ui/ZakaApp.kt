package com.jabulile.zaka.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
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
import com.jabulile.zaka.data.ZakaPreferences
import com.jabulile.zaka.ui.components.MoneyText
import com.jabulile.zaka.ui.components.ShareCsvWhenRequested
import com.jabulile.zaka.ui.components.ZakaIcons
import com.jabulile.zaka.ui.expenses.ExpensesScreen
import com.jabulile.zaka.ui.expenses.ExpensesViewModel
import com.jabulile.zaka.ui.form.ExpenseFormScreen
import com.jabulile.zaka.ui.form.ExpenseFormViewModel
import com.jabulile.zaka.ui.cards.CardCodeScreen
import com.jabulile.zaka.ui.cards.CardFormScreen
import com.jabulile.zaka.ui.cards.CardFormViewModel
import com.jabulile.zaka.ui.cards.CardScannerScreen
import com.jabulile.zaka.ui.cards.CardsScreen
import com.jabulile.zaka.ui.cards.CardsViewModel
import com.jabulile.zaka.ui.insights.InsightsScreen
import com.jabulile.zaka.ui.insights.InsightsViewModel

/**
 * The four tabs, and the month-to-date total that sits above them wherever you
 * are.
 *
 * No navigation library. Four destinations and one modal form do not need a
 * back stack: the tab and the expense being edited are `rememberSaveable`
 * state, which survives rotation and process death, and that is the whole
 * requirement. Revisit this the moment there are deep links or a third level
 * of screen.
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
    preferences: ZakaPreferences,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(ZakaTab.EXPENSES) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var cardShown by rememberSaveable { mutableStateOf<Long?>(null) }
    var cardEdited by rememberSaveable { mutableStateOf<Long?>(null) }
    var addingCard by rememberSaveable { mutableStateOf(false) }

    val cardsViewModel: CardsViewModel = viewModel(factory = CardsViewModel.factory(cards))
    val cardsState by cardsViewModel.state.collectAsStateWithLifecycle()

    // Anything full screen wins over the tabs, in the order it was opened.
    val editedCardId = cardEdited
    if (addingCard || editedCardId != null) {
        EditCard(
            cards = cards,
            cardId = editedCardId,
            onClose = {
                addingCard = false
                cardEdited = null
            },
            modifier = modifier,
        )
        return
    }

    val shownCard = cardShown?.let { id -> cardsState.cards.firstOrNull { it.id == id } }
    if (shownCard != null) {
        BackHandler { cardShown = null }
        Scaffold(modifier = modifier.fillMaxSize()) { insets ->
            CardCodeScreen(
                card = shownCard,
                onClose = { cardShown = null },
                modifier = Modifier.padding(insets),
            )
        }
        return
    }

    val editingId = editing
    if (editingId != null) {
        EditExpense(
            expenses = expenses,
            expenseId = editingId,
            onClose = { editing = null },
            modifier = modifier,
        )
        return
    }

    val expensesViewModel: ExpensesViewModel =
        viewModel(factory = ExpensesViewModel.factory(expenses))
    val expensesState by expensesViewModel.state.collectAsStateWithLifecycle()

    val chooserTitle = stringResource(R.string.export_chooser)
    ShareCsvWhenRequested(
        file = expensesState.export,
        chooserTitle = chooserTitle,
        onShared = expensesViewModel::exportShared,
    )
    ShareCsvWhenRequested(
        file = cardsState.export,
        chooserTitle = chooserTitle,
        onShared = cardsViewModel::exportShared,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                MonthToDate(total = expensesState.monthTotal)
                NavigationBar {
                    ZakaTab.entries.forEach { candidate ->
                        NavigationBarItem(
                            selected = tab == candidate,
                            onClick = { tab = candidate },
                            icon = {
                                Icon(imageVector = candidate.icon, contentDescription = null)
                            },
                            label = { Text(stringResource(candidate.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { insets ->
        val content = Modifier.padding(insets)
        when (tab) {
            ZakaTab.EXPENSES -> ExpensesScreen(
                state = expensesState,
                onEdit = { editing = it },
                onDelete = expensesViewModel::delete,
                modifier = content,
                onExport = expensesViewModel::export,
            )

            ZakaTab.ADD -> AddExpense(
                expenses = expenses,
                onSaved = { tab = ZakaTab.EXPENSES },
                modifier = content,
            )

            ZakaTab.INSIGHTS -> Insights(
                expenses = expenses,
                preferences = preferences,
                modifier = content,
            )
            ZakaTab.CARDS -> CardsScreen(
                state = cardsState,
                onShow = { cardShown = it },
                onEdit = { cardEdited = it },
                onDelete = cardsViewModel::delete,
                onAdd = { addingCard = true },
                modifier = content,
                onExport = cardsViewModel::export,
            )
        }
    }
}

@Composable
private fun Insights(
    expenses: ExpenseRepository,
    preferences: ZakaPreferences,
    modifier: Modifier = Modifier,
) {
    val insights: InsightsViewModel =
        viewModel(factory = InsightsViewModel.factory(expenses, preferences))
    val state by insights.state.collectAsStateWithLifecycle()

    InsightsScreen(
        state = state,
        onPrevious = insights::previousMonth,
        onNext = insights::nextMonth,
        onStyle = insights::styleChosen,
        modifier = modifier,
    )
}

@Composable
private fun AddExpense(
    expenses: ExpenseRepository,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form: ExpenseFormViewModel =
        viewModel(key = "add", factory = ExpenseFormViewModel.factory(expenses, null))
    val state by form.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) {
            // Blank the form before leaving, so coming back to the tab is not
            // the last expense waiting to be saved again.
            form.reset()
            onSaved()
        }
    }

    ExpenseFormScreen(
        state = state,
        onAmountChange = form::amountChanged,
        onCategoryChange = form::categoryChosen,
        onDateChange = form::dateChosen,
        onNoteChange = form::noteChanged,
        onSave = form::save,
        onDelete = form::delete,
        modifier = modifier,
    )
}

@Composable
private fun EditCard(
    cards: LoyaltyCardRepository,
    cardId: Long?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form: CardFormViewModel = viewModel(
        key = "card-${cardId ?: "new"}",
        factory = CardFormViewModel.factory(cards, cardId),
    )
    val state by form.state.collectAsStateWithLifecycle()
    var scanning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.finished) {
        if (state.finished) {
            form.reset()
            onClose()
        }
    }
    BackHandler { if (scanning) scanning = false else onClose() }

    if (scanning) {
        Scaffold(modifier = modifier.fillMaxSize()) { insets ->
            CardScannerScreen(
                onScanned = {
                    form.scanned(it)
                    scanning = false
                },
                onCancel = { scanning = false },
                modifier = Modifier.padding(insets),
            )
        }
        return
    }

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        CardFormScreen(
            state = state,
            onName = form::nameChanged,
            onNumber = form::numberChanged,
            onSymbology = form::symbologyChosen,
            onTint = form::tintChosen,
            onSave = form::save,
            onDelete = form::delete,
            onCancel = onClose,
            modifier = Modifier.padding(insets),
            onScan = { scanning = true },
        )
    }
}

@Composable
private fun EditExpense(
    expenses: ExpenseRepository,
    expenseId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form: ExpenseFormViewModel = viewModel(
        key = "edit-$expenseId",
        factory = ExpenseFormViewModel.factory(expenses, expenseId),
    )
    val state by form.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }
    BackHandler(onBack = onClose)

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        ExpenseFormScreen(
            state = state,
            onAmountChange = form::amountChanged,
            onCategoryChange = form::categoryChosen,
            onDateChange = form::dateChosen,
            onNoteChange = form::noteChanged,
            onSave = form::save,
            onDelete = form::delete,
            onCancel = onClose,
            modifier = Modifier.padding(insets),
        )
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
