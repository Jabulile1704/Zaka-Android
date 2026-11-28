package com.jabulile.zaka.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.SpendBreakdown
import com.jabulile.zaka.core.SpendSlice
import com.jabulile.zaka.data.BreakdownStyle
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.ZakaPreferences
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
    val month: YearMonth = YearMonth.now(),
    val total: Double = 0.0,
    /** Spend per day *so far* in a month still running, per day otherwise. */
    val perDay: Double = 0.0,
    val slices: List<SpendSlice> = emptyList(),
    val style: BreakdownStyle = BreakdownStyle.DONUT,
    val canGoForward: Boolean = false,
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val expenses: ExpenseRepository,
    private val preferences: ZakaPreferences,
    zone: ZoneId = ZoneId.systemDefault(),
    /**
     * One clock, read in several places. Taking "now" from the system five
     * separate times meant the month being shown, the month allowed and the
     * divisor for the daily rate could each disagree across a boundary.
     */
    private val clock: Clock = Clock.system(zone),
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))
    private val style = MutableStateFlow(preferences.breakdownStyle)

    /**
     * The month and its totals travel together.
     *
     * Combining the month with a separate flow of totals let the two
     * disagree: changing month emitted the new month's name immediately,
     * while the query behind the totals was still running, so for a frame the
     * screen showed one month's heading above another month's figures.
     * Pairing first and querying inside makes that state unrepresentable.
     */
    val state: StateFlow<InsightsUiState> =
        combine(month, style) { month, style -> month to style }
            .flatMapLatest { (month, style) ->
                expenses.observeMonthBreakdown(month).map { totals ->
                    val total = totals.values.sum()
                    InsightsUiState(
                        month = month,
                        total = total,
                        perDay = total / daysCounted(month),
                        slices = SpendBreakdown.slices(totals),
                        style = style,
                        canGoForward = month < YearMonth.now(clock),
                        loaded = true,
                    )
                }
            }
            .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = InsightsUiState(month = YearMonth.now(clock), style = style.value),
        )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        if (month.value < YearMonth.now(clock)) month.value = month.value.plusMonths(1)
    }

    fun styleChosen(chosen: BreakdownStyle) {
        preferences.breakdownStyle = chosen
        style.value = chosen
    }

    /**
     * Dividing by the whole of a month that is still running would understate
     * the daily rate every day except the last, so the current month counts
     * only the days that have happened.
     */
    private fun daysCounted(month: YearMonth): Int {
        val today = LocalDate.now(clock)
        return if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            expenses: ExpenseRepository,
            preferences: ZakaPreferences,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { InsightsViewModel(expenses, preferences) }
        }
    }
}
