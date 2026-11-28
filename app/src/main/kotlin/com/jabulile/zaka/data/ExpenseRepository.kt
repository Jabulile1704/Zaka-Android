package com.jabulile.zaka.data

import com.jabulile.zaka.core.DayGroup
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.MonthWindow
import com.jabulile.zaka.core.SpendBreakdown
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Everything the screens need from the expense store, already in the shape
 * they need it.
 *
 * Grouping and totalling happen here rather than in a composable: the list
 * screen gets days that are already bucketed and already added up, so
 * recomposition never re-folds the whole month.
 */
class ExpenseRepository(
    private val dao: ExpenseDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    fun observeDays(): Flow<List<DayGroup>> =
        dao.observeAll().map { rows -> DayGroup.group(rows.map { it.toDomain() }, zone) }

    fun observeMonth(month: YearMonth): Flow<List<Expense>> =
        MonthWindow.of(month, zone).let { window ->
            dao.observeBetween(window.start.toEpochMilli(), window.endExclusive.toEpochMilli())
                .map { rows -> rows.map { it.toDomain() } }
        }

    fun observeMonthTotal(month: YearMonth): Flow<Double> =
        MonthWindow.of(month, zone).let { window ->
            dao.observeTotalBetween(
                window.start.toEpochMilli(),
                window.endExclusive.toEpochMilli(),
            )
        }

    fun observeMonthBreakdown(month: YearMonth): Flow<Map<ExpenseCategory, Double>> =
        observeMonth(month).map { SpendBreakdown.totals(it) }

    suspend fun find(id: Long): Expense? = dao.find(id)?.toDomain()

    /**
     * Inserts a new expense or updates an existing one, and returns its id.
     * An id of zero means new, which is what the add form hands over.
     */
    suspend fun save(expense: Expense): Long =
        if (expense.id == 0L) {
            dao.insert(expense.toEntity())
        } else {
            dao.update(expense.toEntity())
            expense.id
        }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
