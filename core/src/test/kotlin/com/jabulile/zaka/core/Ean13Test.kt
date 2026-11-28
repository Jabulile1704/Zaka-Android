package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ean13Test {
    @Test
    fun `check digits`() {
        assertEquals(1, Ean13.checkDigit(digits("400638133393")))
        assertEquals(7, Ean13.checkDigit(digits("590123412345")))
        assertEquals(4, Ean13.checkDigit(digits("978020137962")))
        assertEquals(8, Ean13.checkDigit(digits("600108700161")))
    }

    @Test
    fun `check digit rejects the wrong length`() {
        assertNull(Ean13.checkDigit(digits("12345")))
    }

    @Test
    fun validity() {
        assertTrue(Ean13.isValid("4006381333931"))
        assertTrue(Ean13.isValid("5901 2341 23457"))
        assertFalse(Ean13.isValid("5901234123451"))
        assertFalse(Ean13.isValid("12345"))
    }

    @Test
    fun `encode and decode round trip`() {
        val codes = listOf(
            "4006381333931",
            "5901234123457",
            "9780201379624",
            "6001087001618",
            "0000000000000",
        )
        for (code in codes) {
            val modules = Ean13.modules(code)
            assertNotNull("$code should encode", modules)
            assertEquals(Ean13.MODULE_COUNT, modules!!.size)
            assertEquals("$code should survive a round trip", code, decode(modules))
        }
    }

    @Test
    fun `guard patterns`() {
        val bits = Ean13.modules("5901234123457")!!.joinToString("") { if (it) "1" else "0" }
        assertEquals("101", bits.substring(0, 3))
        assertEquals("01010", bits.substring(45, 50))
        assertEquals("101", bits.substring(92, 95))
    }

    @Test
    fun `an invalid code produces no barcode`() {
        assertNull(Ean13.modules("5901234123451"))
        assertNull(Ean13.modules("not a barcode"))
    }

    private fun digits(text: String): List<Int> = text.map { it - '0' }

    /** Decodes independently of the encoder, so a shared mistake cannot pass. */
    private fun decode(modules: List<Boolean>): String? {
        if (modules.size != MODULE_COUNT) return null
        val bits = modules.joinToString("") { if (it) "1" else "0" }
        fun slice(from: Int, to: Int) = bits.substring(from, to)

        if (slice(0, 3) != "101" || slice(45, 50) != "01010" || slice(92, 95) != "101") {
            return null
        }

        val parity = StringBuilder()
        val body = mutableListOf<Int>()
        for (index in 0 until 6) {
            val chunk = slice(3 + index * 7, 10 + index * 7)
            val odd = LEFT_ODD.indexOf(chunk)
            val even = LEFT_EVEN.indexOf(chunk)
            when {
                odd >= 0 -> { parity.append('L'); body += odd }
                even >= 0 -> { parity.append('G'); body += even }
                else -> return null
            }
        }
        val first = PARITY.indexOf(parity.toString())
        if (first < 0) return null

        val tail = mutableListOf<Int>()
        for (index in 0 until 6) {
            val chunk = slice(50 + index * 7, 57 + index * 7)
            val digit = RIGHT.indexOf(chunk)
            if (digit < 0) return null
            tail += digit
        }

        return (listOf(first) + body + tail).joinToString("")
    }

    private companion object {
        const val MODULE_COUNT = 95

        val LEFT_ODD = listOf(
            "0001101", "0011001", "0010011", "0111101", "0100011",
            "0110001", "0101111", "0111011", "0110111", "0001011",
        )
        val LEFT_EVEN = listOf(
            "0100111", "0110011", "0011011", "0100001", "0011101",
            "0111001", "0000101", "0010001", "0001001", "0010111",
        )
        val RIGHT = listOf(
            "1110010", "1100110", "1101100", "1000010", "1011100",
            "1001110", "1010000", "1000100", "1001000", "1110100",
        )
        val PARITY = listOf(
            "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
            "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL",
        )
    }
}
