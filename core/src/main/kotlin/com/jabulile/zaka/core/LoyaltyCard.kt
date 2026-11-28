package com.jabulile.zaka.core

import java.time.Instant

/**
 * A shop loyalty card, as the app thinks about it.
 *
 * Loyalty cards only — never a payment card. The store is unencrypted SQLite,
 * so anything in it is readable from an unlocked phone or a device backup.
 */
data class LoyaltyCard(
    val id: Long = 0,
    val name: String,
    val number: String,
    val symbology: BarcodeSymbology,
    val tintHex: String = CardTint.palette[0],
    val addedAt: Instant,
) {
    /** What the card list shows instead of the whole number. */
    val lastFour: String get() = CardNumber.lastFour(number)
}
