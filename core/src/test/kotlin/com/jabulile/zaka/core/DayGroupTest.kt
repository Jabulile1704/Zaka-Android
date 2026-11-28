package com.jabulile.zaka.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DayGroupTest {
    private val johannesburg: ZoneId = ZoneId.of("Africa/Johannesburg")

    @Test
    fun `expenses are bucketed by calendar day, newest day first`() {
        val groups = DayGroup.group(
            listOf(
                expense(10.0, "2026-11-20T08:00:00Z"),
                expense(20.0, "2026-11-22T06:00:00Z"),
                expense(30.0, "2026-11-22T18:00:00Z"),
            ),
            johannesburg,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 20)),
            groups.map { it.day },
        )
        assertEquals(50.0, groups[0].total, 1e-9)
        assertEquals(10.0, groups[1].total, 1e-9)
    }

    @Test
    fun `the newest expense of a day comes first`() {
        val groups = DayGroup.group(
            listOf(
                expense(10.0, "2026-11-22T06:00:00Z"),
                expense(30.0, "2026-11-22T18:00:00Z"),
            ),
            johannesburg,
        )
        assertEquals(listOf(30.0, 10.0), groups.single().expenses.map { it.amount })
    }

    @Test
    fun `the day boundary follows the zone, not UTC`() {
        // 23:30 UTC is already the next morning in Johannesburg (UTC+2).
        val groups = DayGroup.group(listOf(expense(10.0, "2026-11-22T23:30:00Z")), johannesburg)
        assertEquals(LocalDate.of(2026, 11, 23), groups.single().day)
    }

    @Test
    fun `headings name today and yesterday and date the rest`() {
        val today = LocalDate.of(2026, 11, 25)
        fun title(day: LocalDate) = DayGroup(day, emptyList()).title(today, Locale.UK)

        assertEquals("Today", title(today))
        assertEquals("Yesterday", title(today.minusDays(1)))
        assertEquals("Fri 20 Nov", title(LocalDate.of(2026, 11, 20)))
        assertEquals("20 Nov 2025", title(LocalDate.of(2025, 11, 20)))
    }

    private fun expense(amount: Double, at: String) = Expense(
        amount = amount,
        category = ExpenseCategory.GROCERIES,
        date = Instant.parse(at),
    )
}
