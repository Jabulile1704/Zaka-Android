package com.jabulile.zaka.core

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthWindowTest {
    private val johannesburg: ZoneId = ZoneId.of("Africa/Johannesburg")

    @Test
    fun `a month starts at local midnight, not UTC midnight`() {
        val window = MonthWindow.of(YearMonth.of(2026, 11), johannesburg)
        assertEquals(Instant.parse("2026-10-31T22:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-11-30T22:00:00Z"), window.endExclusive)
    }

    @Test
    fun `the range is half-open`() {
        val window = MonthWindow.of(YearMonth.of(2026, 11), johannesburg)
        assertTrue(window.start in window)
        assertTrue(Instant.parse("2026-11-30T21:59:59.999Z") in window)
        assertFalse("the first instant of December is not November", window.endExclusive in window)
    }

    @Test
    fun `December rolls into the next year`() {
        val window = MonthWindow.of(YearMonth.of(2026, 12), johannesburg)
        assertEquals(Instant.parse("2026-11-30T22:00:00Z"), window.start)
        assertEquals(Instant.parse("2026-12-31T22:00:00Z"), window.endExclusive)
    }

    @Test
    fun `the window containing a moment is the month it falls in locally`() {
        // 23:30 UTC on the last of October is already November in Johannesburg.
        val window = MonthWindow.containing(Instant.parse("2026-10-31T23:30:00Z"), johannesburg)
        assertEquals(MonthWindow.of(YearMonth.of(2026, 11), johannesburg), window)
    }
}
