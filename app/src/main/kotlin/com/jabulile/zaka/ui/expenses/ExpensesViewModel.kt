package com.jabulile.zaka.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.DayGroup
import com.jabulile.zaka.data.ExpenseRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One immutable state for the whole screen.
 *
 * [days] arrives already bucketed and already totalled, and [today] comes with
 * it so a day heading never has to ask the clock during composition.
 */
data class ExpensesUiState(
    val days: List<DayGroup> = emptyList(),
    val monthTotal: Double = 0.0,
    val today: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
)

class ExpensesViewModel(
    private val expenses: ExpenseRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    val state: StateFlow<ExpensesUiState> =
        combine(
            expenses.observeDays(),
            expenses.observeMonthTotal(YearMonth.now(zone)),
        ) { days, monthTotal ->
            ExpensesUiState(
                days = days,
                monthTotal = monthTotal,
                today = LocalDate.now(zone),
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExpensesUiState(),
        )

    fun delete(id: Long) {
        viewModelScope.launch { expenses.delete(id) }
    }

    companion object {
        /** Long enough to survive a rotation without re-querying. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(expenses: ExpenseRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExpensesViewModel(expenses) }
        }
    }
}
