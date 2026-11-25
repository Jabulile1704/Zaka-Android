package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategoryTest {
    @Test
    fun `live values map to themselves`() {
        assertEquals(ExpenseCategory.TAKEAWAYS, ExpenseCategory.stored("takeaways"))
        assertEquals(ExpenseCategory.GROCERIES, ExpenseCategory.stored("groceries"))
        assertEquals(ExpenseCategory.TRANSPORT, ExpenseCategory.stored("transport"))
        assertEquals(ExpenseCategory.UTILITIES, ExpenseCategory.stored("utilities"))
        assertEquals(ExpenseCategory.OTHERS, ExpenseCategory.stored("other"))
    }

    @Test
    fun `retired values map to their replacement`() {
        assertEquals(ExpenseCategory.TAKEAWAYS, ExpenseCategory.stored("food"))
        assertEquals(ExpenseCategory.ACCOUNTS, ExpenseCategory.stored("bills"))
        assertEquals(ExpenseCategory.OTHERS, ExpenseCategory.stored("fun"))
    }

    @Test
    fun `an unknown value falls back rather than crashing`() {
        assertEquals(ExpenseCategory.OTHERS, ExpenseCategory.stored("nonsense"))
        assertEquals(ExpenseCategory.OTHERS, ExpenseCategory.stored(""))
    }

    @Test
    fun `every category has a label, an icon, a colour and a distinct raw value`() {
        val raws = ExpenseCategory.entries.map { it.raw }.toSet()
        assertEquals(ExpenseCategory.entries.size, raws.size)
        for (category in ExpenseCategory.entries) {
            assertTrue(category.label.isNotEmpty())
            assertTrue(category.iconName.isNotEmpty())
            assertTrue(
                "${category.raw} needs a six-digit sRGB colour",
                category.colorHex.length == 6 && category.colorHex.all { it.isDigit() || it in 'A'..'F' },
            )
        }
    }
}
