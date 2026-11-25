package com.jabulile.zaka.core

/**
 * EAN-13: check digit, validation, and the 95 modules of the barcode itself.
 *
 * Drawn by hand because the number has to survive a till scanner: a module
 * list renders as vector bars at any size, where a scaled bitmap scans badly.
 */
object Ean13 {
    const val MODULE_COUNT: Int = 95

    private val LEFT_ODD = listOf(
        "0001101", "0011001", "0010011", "0111101", "0100011",
        "0110001", "0101111", "0111011", "0110111", "0001011",
    )

    private val LEFT_EVEN = listOf(
        "0100111", "0110011", "0011011", "0100001", "0011101",
        "0111001", "0000101", "0010001", "0001001", "0010111",
    )

    private val RIGHT = listOf(
        "1110010", "1100110", "1101100", "1000010", "1011100",
        "1001110", "1010000", "1000100", "1001000", "1110100",
    )

    private val PARITY = listOf(
        "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
        "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL",
    )

    fun checkDigit(first12: List<Int>): Int? {
        if (first12.size != 12 || first12.any { it !in 0..9 }) return null
        val sum = first12.withIndex().sumOf { (index, digit) ->
            digit * if (index % 2 == 0) 1 else 3
        }
        return (10 - sum % 10) % 10
    }

    fun isValid(text: String): Boolean {
        val digits = digits(text) ?: return false
        if (digits.size != 13) return false
        return checkDigit(digits.take(12)) == digits[12]
    }

    /** The bars, left to right: `true` is a dark module. */
    fun modules(text: String): List<Boolean>? {
        val digits = digits(text) ?: return null
        if (digits.size != 13) return null
        if (checkDigit(digits.take(12)) != digits[12]) return null

        val bits = StringBuilder("101")

        val pattern = PARITY[digits[0]]
        digits.subList(1, 7).forEachIndexed { index, digit ->
            bits.append(if (pattern[index] == 'L') LEFT_ODD[digit] else LEFT_EVEN[digit])
        }

        bits.append("01010")

        digits.subList(7, 13).forEach { bits.append(RIGHT[it]) }

        bits.append("101")

        if (bits.length != MODULE_COUNT) return null
        return bits.map { it == '1' }
    }

    private fun digits(text: String): List<Int>? {
        val cleaned = text.filter { it.isAsciiDigit() }
        if (cleaned.isEmpty()) return null
        return cleaned.map { it - '0' }
    }
}
