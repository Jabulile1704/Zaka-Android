package com.jabulile.zaka.core

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseTest {
    @Test
    fun `the title falls back to the category when there is no note`() {
        assertEquals("Rent", expense(note = "").title)
        assertEquals("whitespace is not a note", "Rent", expense(note = "   ").title)
        assertEquals("October", expense(note = "October").title)
    }

    @Test
    fun `a note of whitespace does not count as one`() {
        assertFalse(expense(note = "").hasNote)
        assertFalse(expense(note = " \n ").hasNote)
        assertTrue(expense(note = "Uber home").hasNote)
    }

    @Test
    fun `a loyalty card shows only its last four digits`() {
        val card = LoyaltyCard(
            name = "Clicks",
            number = "6001087001618",
            symbology = BarcodeSymbology.EAN13,
            addedAt = Instant.EPOCH,
        )
        assertEquals("1618", card.lastFour)
    }

    private fun expense(note: String) = Expense(
        amount = 1.0,
        category = ExpenseCategory.RENT,
        date = Instant.EPOCH,
        note = note,
    )
}
