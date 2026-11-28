package com.jabulile.zaka.core

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendBreakdownTest {
    @Test
    fun `totals sum per category`() {
        val totals = SpendBreakdown.totals(
            listOf(
                expense(100.0, ExpenseCategory.TAKEAWAYS),
                expense(50.0, ExpenseCategory.TAKEAWAYS),
                expense(25.0, ExpenseCategory.TRANSPORT),
            ),
        )
        assertEquals(150.0, totals[ExpenseCategory.TAKEAWAYS]!!, DELTA)
        assertEquals(25.0, totals[ExpenseCategory.TRANSPORT]!!, DELTA)
        assertNull(totals[ExpenseCategory.RENT])
    }

    @Test
    fun `fewer categories than the limit are all kept`() {
        val slices = SpendBreakdown.slices(
            mapOf(ExpenseCategory.RENT to 3700.0, ExpenseCategory.TRANSPORT to 790.0),
            limit = 6,
        )
        assertEquals(2, slices.size)
        assertFalse(slices.any { it.isMerged })
        assertEquals(listOf(ExpenseCategory.RENT, ExpenseCategory.TRANSPORT), slices.map { it.category })
    }

    @Test
    fun `exactly the limit is not merged`() {
        val totals = mapOf(
            ExpenseCategory.RENT to 6.0,
            ExpenseCategory.TRANSPORT to 5.0,
            ExpenseCategory.TAKEAWAYS to 4.0,
            ExpenseCategory.GROCERIES to 3.0,
            ExpenseCategory.WIFI to 2.0,
            ExpenseCategory.AIRTIME to 1.0,
        )
        val slices = SpendBreakdown.slices(totals, limit = 6)
        assertEquals(6, slices.size)
        assertFalse(slices.any { it.isMerged })
    }

    @Test
    fun `over the limit merges the smallest`() {
        val totals = mapOf(
            ExpenseCategory.RENT to 7.0,
            ExpenseCategory.TRANSPORT to 6.0,
            ExpenseCategory.TAKEAWAYS to 5.0,
            ExpenseCategory.GROCERIES to 4.0,
            ExpenseCategory.WIFI to 3.0,
            ExpenseCategory.AIRTIME to 2.0,
            ExpenseCategory.HAIRCUT to 1.0,
        )
        val slices = SpendBreakdown.slices(totals, limit = 6)

        assertEquals("never more slices than the limit", 6, slices.size)
        assertEquals(
            listOf(
                ExpenseCategory.RENT,
                ExpenseCategory.TRANSPORT,
                ExpenseCategory.TAKEAWAYS,
                ExpenseCategory.GROCERIES,
                ExpenseCategory.WIFI,
            ),
            slices.take(5).map { it.category },
        )

        val last = slices[5]
        assertTrue(last.isMerged)
        assertEquals("airtime and haircut combined", 3.0, last.amount, DELTA)
        assertEquals(2, last.mergedCount)
        assertEquals("+2 more", last.label)
    }

    @Test
    fun `merging preserves the total`() {
        val totals = mapOf(
            ExpenseCategory.RENT to 7.0,
            ExpenseCategory.TRANSPORT to 6.0,
            ExpenseCategory.TAKEAWAYS to 5.0,
            ExpenseCategory.GROCERIES to 4.0,
            ExpenseCategory.WIFI to 3.0,
            ExpenseCategory.AIRTIME to 2.0,
            ExpenseCategory.HAIRCUT to 1.0,
            ExpenseCategory.DATES to 10.0,
        )
        val expected = totals.values.sum()
        val sliced = SpendBreakdown.slices(totals, limit = 4).sumOf { it.amount }
        assertEquals(expected, sliced, 0.0001)
    }

    @Test
    fun `zero and negative amounts are dropped`() {
        val slices = SpendBreakdown.slices(
            mapOf(
                ExpenseCategory.RENT to 100.0,
                ExpenseCategory.WIFI to 0.0,
                ExpenseCategory.AIRTIME to -5.0,
            ),
        )
        assertEquals(listOf(ExpenseCategory.RENT), slices.map { it.category })
    }

    @Test
    fun `ties are ordered predictably`() {
        val first = SpendBreakdown.slices(
            mapOf(
                ExpenseCategory.WIFI to 50.0,
                ExpenseCategory.AIRTIME to 50.0,
                ExpenseCategory.RENT to 50.0,
            ),
        )
        val again = SpendBreakdown.slices(
            mapOf(
                ExpenseCategory.RENT to 50.0,
                ExpenseCategory.WIFI to 50.0,
                ExpenseCategory.AIRTIME to 50.0,
            ),
        )
        assertEquals(
            "equal amounts must not reorder run to run",
            first.map { it.id },
            again.map { it.id },
        )
        assertEquals(
            listOf(ExpenseCategory.AIRTIME, ExpenseCategory.RENT, ExpenseCategory.WIFI),
            first.map { it.category },
        )
    }

    @Test
    fun `empty and degenerate input`() {
        assertTrue(SpendBreakdown.slices(emptyMap()).isEmpty())
        assertTrue(SpendBreakdown.slices(mapOf(ExpenseCategory.RENT to 100.0), limit = 0).isEmpty())
        assertEquals(1, SpendBreakdown.slices(mapOf(ExpenseCategory.RENT to 100.0), limit = 1).size)
    }

    @Test
    fun `share of the total`() {
        val slice = SpendSlice(ExpenseCategory.RENT, 25.0)
        assertEquals(0.25, slice.share(100.0), DELTA)
        assertEquals("no division by zero on an empty month", 0.0, slice.share(0.0), DELTA)
    }

    @Test
    fun `sweep always fills the circle`() {
        val slices = listOf(
            SpendSlice(ExpenseCategory.RENT, 300.0),
            SpendSlice(ExpenseCategory.WIFI, 100.0),
        )
        for (progress in listOf(0.0, 0.1, 0.5, 0.99, 1.0)) {
            val sweep = SpendBreakdown.sweep(slices, progress)
            assertEquals(
                "the donut must stay a full circle at progress $progress",
                400.0,
                sweep.drawn.sum() + sweep.remainder,
                0.0001,
            )
        }
    }

    @Test
    fun `sweep starts empty and ends complete`() {
        val slices = listOf(SpendSlice(ExpenseCategory.RENT, 300.0))

        val start = SpendBreakdown.sweep(slices, 0.0)
        assertEquals(listOf(0.0), start.drawn)
        assertEquals(300.0, start.remainder, DELTA)

        val end = SpendBreakdown.sweep(slices, 1.0)
        assertEquals(listOf(300.0), end.drawn)
        assertEquals(0.0, end.remainder, 0.0001)
    }

    @Test
    fun `sweep clamps out-of-range progress`() {
        val slices = listOf(SpendSlice(ExpenseCategory.RENT, 300.0))
        assertEquals(listOf(0.0), SpendBreakdown.sweep(slices, -2.0).drawn)
        assertEquals(listOf(300.0), SpendBreakdown.sweep(slices, 5.0).drawn)
    }

    private fun expense(amount: Double, category: ExpenseCategory) =
        Expense(amount = amount, category = category, date = Instant.EPOCH)

    private companion object {
        const val DELTA = 1e-9
    }
}
