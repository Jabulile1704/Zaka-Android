package com.jabulile.zaka.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.data.ExpenseRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseFormUiState(
    val amountText: String = "",
    val category: ExpenseCategory = ExpenseCategory.TAKEAWAYS,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val editing: Boolean = false,
    val loaded: Boolean = false,
    /** Set once the form has done its job and the caller should close it. */
    val finished: Boolean = false,
) {
    /** Null while what has been typed is not yet a number. */
    val amount: Double? get() = Money.parse(amountText)

    val canSave: Boolean get() = (amount ?: 0.0) > 0.0
}

/**
 * Add and edit are the same form in two modes, as on iOS. [expenseId] is null
 * when adding.
 */
class ExpenseFormViewModel(
    private val expenses: ExpenseRepository,
    private val expenseId: Long?,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    /**
     * The time of day the expense carries, which the form never shows.
     *
     * Editing keeps whatever the expense already had, so changing an amount
     * does not move the row to the bottom of its day. A new expense gets the
     * clock, which is what makes "newest first" mean anything within a day.
     */
    private var timeOfDay: LocalTime = LocalTime.now(zone)

    private val _state = MutableStateFlow(
        ExpenseFormUiState(
            date = LocalDate.now(zone),
            editing = expenseId != null,
            loaded = expenseId == null,
        ),
    )
    val state: StateFlow<ExpenseFormUiState> = _state.asStateFlow()

    init {
        if (expenseId != null) load(expenseId)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val existing = expenses.find(id)
            if (existing == null) {
                // Deleted from under us, most likely by a swipe on the list.
                _state.update { it.copy(loaded = true, finished = true) }
                return@launch
            }
            val local = existing.date.atZone(zone)
            timeOfDay = local.toLocalTime()
            _state.value = ExpenseFormUiState(
                amountText = plainAmount(existing.amount),
                category = existing.category,
                date = local.toLocalDate(),
                note = existing.note,
                editing = true,
                loaded = true,
            )
        }
    }

    fun amountChanged(text: String) = _state.update { it.copy(amountText = text) }

    fun categoryChosen(category: ExpenseCategory) = _state.update { it.copy(category = category) }

    fun dateChosen(date: LocalDate) = _state.update { it.copy(date = date) }

    fun noteChanged(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val current = _state.value
        val amount = current.amount ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            expenses.save(
                Expense(
                    id = expenseId ?: 0,
                    amount = amount,
                    category = current.category,
                    date = current.date.atTime(timeOfDay).atZone(zone).toInstant(),
                    note = current.note.trim(),
                ),
            )
            _state.update { it.copy(finished = true) }
        }
    }

    /**
     * Back to a blank form, for the Add tab after a save. Resetting beats
     * creating a second view model under a new key, which would leave the
     * first one in the activity's store for the rest of the session.
     */
    fun reset() {
        timeOfDay = LocalTime.now(zone)
        _state.value = ExpenseFormUiState(date = LocalDate.now(zone), loaded = true)
    }

    fun delete() {
        val id = expenseId ?: return
        viewModelScope.launch {
            expenses.delete(id)
            _state.update { it.copy(finished = true) }
        }
    }

    companion object {
        /**
         * Digits and a dot, never `R1,234.56`: this goes back into the field
         * the user types in, and [Money.parse] has to read it again.
         */
        private fun plainAmount(amount: Double): String =
            String.format(Locale.US, "%.2f", amount)

        fun factory(
            expenses: ExpenseRepository,
            expenseId: Long?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExpenseFormViewModel(expenses, expenseId) }
        }
    }
}
