package com.jabulile.zaka.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Expenses for one calendar day, newest first, with that day's total. */
data class DayGroup(
    val day: LocalDate,
    val expenses: List<Expense>,
) {
    val total: Double get() = expenses.sumOf { it.amount }

    /**
     * The day heading. Today and yesterday are named; everything else is a
     * date, and the year appears only when it is not the current one.
     */
    fun title(
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
    ): String = when {
        day == today -> "Today"
        day == today.minusDays(1) -> "Yesterday"
        day.year == today.year -> day.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
        else -> day.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
    }

    companion object {
        /** Buckets expenses into calendar days in [zone], newest day first. */
        fun group(
            expenses: List<Expense>,
            zone: ZoneId = ZoneId.systemDefault(),
        ): List<DayGroup> = expenses
            .groupBy { it.date.dayIn(zone) }
            .map { (day, items) -> DayGroup(day, items.sortedByDescending { it.date }) }
            .sortedByDescending { it.day }

        private fun Instant.dayIn(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()
    }
}
