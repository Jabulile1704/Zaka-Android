package com.jabulile.zaka.core

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Rand formatting and keypad parsing.
 *
 * Display is always `R1,234.56`: the grouping and decimal separators are fixed
 * to the en-US convention rather than taken from the device, so a screenshot
 * reads the same on every phone. Input is the opposite — it accepts whichever
 * separator the keyboard offers.
 */
object Money {
    const val SYMBOL: String = "R"

    fun string(value: Double): String = SYMBOL + format("#,##0.00").format(value)

    fun rounded(value: Double): String = SYMBOL + format("#,##0").format(value)

    /**
     * Reads an amount typed on a keypad. Both `.` and `,` are taken as the
     * decimal point when one or two digits follow, and as grouping otherwise:
     * `45,50` is 45.50 while `1,234` is 1234.
     */
    fun parse(text: String): Double? {
        val cleaned = text.filter { it.isAsciiDigit() || it == '.' || it == ',' }
        if (cleaned.isEmpty()) return null

        var whole = cleaned
        var fraction = ""

        val separator = cleaned.indexOfLast { it == '.' || it == ',' }
        if (separator >= 0) {
            val tail = cleaned.substring(separator + 1)
            if (tail.length in 1..2 && tail.all { it.isAsciiDigit() }) {
                whole = cleaned.substring(0, separator)
                fraction = tail
            }
        }

        val digits = whole.filter { it.isAsciiDigit() }
        if (digits.isEmpty() && fraction.isEmpty()) return null

        return "${digits.ifEmpty { "0" }}.${fraction.ifEmpty { "0" }}".toDoubleOrNull()
    }

    private fun format(pattern: String) =
        DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).apply {
            roundingMode = RoundingMode.HALF_EVEN
        }
}

internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
