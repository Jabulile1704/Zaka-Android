package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun `dot and comma both mean a decimal point`() {
        assertEquals(45.50, Money.parse("45.50")!!, DELTA)
        assertEquals(45.50, Money.parse("45,50")!!, DELTA)
    }

    @Test
    fun `grouping is not mistaken for decimals`() {
        assertEquals(1234.0, Money.parse("1,234")!!, DELTA)
        assertEquals(1234.56, Money.parse("1,234.56")!!, DELTA)
        assertEquals(1234.56, Money.parse("1.234,56")!!, DELTA)
        assertEquals(1234567.89, Money.parse("1,234,567.89")!!, DELTA)
    }

    @Test
    fun `partial input`() {
        assertEquals(45.0, Money.parse("45")!!, DELTA)
        assertEquals(45.0, Money.parse("45.")!!, DELTA)
        assertEquals(0.5, Money.parse(".50")!!, DELTA)
        assertEquals(0.5, Money.parse(",5")!!, DELTA)
        assertEquals(0.0, Money.parse("0")!!, DELTA)
    }

    @Test
    fun `rejects non-numbers`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun `formatting is rand with dot decimals`() {
        assertEquals("R1,234.56", Money.string(1234.56))
        assertEquals("R45.50", Money.string(45.5))
        assertEquals("R1,235", Money.rounded(1234.56))
    }

    private companion object {
        const val DELTA = 1e-9
    }
}
