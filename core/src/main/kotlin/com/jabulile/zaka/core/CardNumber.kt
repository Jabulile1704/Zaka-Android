package com.jabulile.zaka.core

/** The part of a loyalty card that is safe to show in a list. */
object CardNumber {
    /** The last four digits, or every digit there is when there are fewer. */
    fun lastFour(number: String): String {
        val digits = number.filter { it.isAsciiDigit() }
        return if (digits.length > 4) digits.takeLast(4) else digits
    }
}

/** The palette a card tile is tinted from. */
object CardTint {
    val palette: List<String> = listOf(
        "0F8E6F", "1E64B4", "B3246B", "C2410C",
        "6D28D9", "0E7490", "B45309", "334155",
    )
}
