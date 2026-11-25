package com.jabulile.zaka.data

import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.LoyaltyCard
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** No Room and no Android here: the mapping is a pure function both ways. */
class MappersTest {
    @Test
    fun `an expense survives the round trip`() {
        val expense = Expense(
            id = 7,
            amount = 249.90,
            category = ExpenseCategory.TRANSPORT,
            date = Instant.parse("2026-11-22T17:45:12.345Z"),
            note = "Uber home",
        )
        assertEquals(expense, expense.toEntity().toDomain())
    }

    @Test
    fun `the column holds the raw category, not the enum name`() {
        val entity = Expense(
            amount = 1.0,
            category = ExpenseCategory.OTHERS,
            date = Instant.EPOCH,
        ).toEntity()
        assertEquals("other", entity.categoryRaw)
    }

    @Test
    fun `a retired category is mapped on read`() {
        val stored = ExpenseEntity(
            id = 1,
            amount = 10.0,
            categoryRaw = "food",
            dateEpochMillis = 0,
            note = "",
        )
        assertEquals(ExpenseCategory.TAKEAWAYS, stored.toDomain().category)
        assertEquals("reading must not rewrite the row", "food", stored.categoryRaw)
    }

    @Test
    fun `an unreadable category does not throw`() {
        val stored = ExpenseEntity(
            amount = 1.0,
            categoryRaw = "nonsense-from-a-future-version",
            dateEpochMillis = 0,
            note = "",
        )
        assertEquals(ExpenseCategory.OTHERS, stored.toDomain().category)
    }

    @Test
    fun `milliseconds are not lost`() {
        val moment = Instant.parse("2026-11-22T17:45:12.345Z")
        assertEquals(moment, ExpenseEntity(
            amount = 1.0,
            categoryRaw = "rent",
            dateEpochMillis = moment.toEpochMilli(),
            note = "",
        ).toDomain().date)
    }

    @Test
    fun `a card survives the round trip`() {
        val card = LoyaltyCard(
            id = 3,
            name = "Dis-Chem",
            number = "6001087001618",
            symbology = BarcodeSymbology.QR,
            tintHex = "B3246B",
            addedAt = Instant.parse("2026-11-01T06:00:00Z"),
        )
        assertEquals(card, card.toEntity().toDomain())
        assertEquals("qr", card.toEntity().symbologyRaw)
    }

    @Test
    fun `an unreadable symbology falls back rather than crashing the card list`() {
        val stored = LoyaltyCardEntity(
            name = "Shop",
            number = "12345",
            symbologyRaw = "nonsense",
            tintHex = "334155",
            addedAtEpochMillis = 0,
        )
        assertEquals(BarcodeSymbology.CODE128, stored.toDomain().symbology)
    }
}
