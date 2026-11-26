package com.jabulile.zaka.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.CsvExport
import com.jabulile.zaka.core.CsvFile
import com.jabulile.zaka.core.DayGroup
import com.jabulile.zaka.data.ExpenseRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    /**
     * Required rather than defaulted to `LocalDate.now()`: a default would
     * read the system zone while everything else here reads the injected
     * clock, which is the sort of disagreement that only shows up abroad.
     */
    val today: LocalDate,
    val loaded: Boolean = false,
    /** Set when an export is ready for the share sheet. */
    val export: CsvFile? = null,
)

class ExpensesViewModel(
    private val expenses: ExpenseRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.system(zone),
) : ViewModel() {
    /**
     * The date is read inside the flow, not when this view model is built.
     *
     * Reading it once at construction pinned the month-to-date total to
     * whichever month the process started in: leave the app open overnight on
     * the first and the strip above the tabs keeps totalling last month while
     * the list above it shows this one. Day headings went stale the same way.
     *
     * `WhileSubscribed` restarts this block when the screen is observed again,
     * so returning to the app after midnight re-reads both.
     */
    private val pending = MutableStateFlow<CsvFile?>(null)

    val state: StateFlow<ExpensesUiState> =
        flow {
            val today = LocalDate.now(clock)
            emitAll(
                combine(
                    expenses.observeDays(),
                    expenses.observeMonthTotal(YearMonth.from(today)),
                    pending,
                ) { days, monthTotal, export ->
                    ExpensesUiState(
                        days = days,
                        monthTotal = monthTotal,
                        today = today,
                        loaded = true,
                        export = export,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExpensesUiState(today = LocalDate.now(clock)),
        )

    fun delete(id: Long) {
        viewModelScope.launch { expenses.delete(id) }
    }

    /**
     * Everything logged, as CSV. Built from the state the screen already
     * holds rather than from a second query: the list is the whole table.
     */
    fun export() {
        val all = state.value.days.flatMap { it.expenses }
        pending.value = CsvExport.expenses(all, zone, LocalDate.now(clock))
    }

    fun exportShared() {
        pending.value = null
    }

    companion object {
        /** Long enough to survive a rotation without re-querying. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(expenses: ExpenseRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExpensesViewModel(expenses) }
        }
    }
}
