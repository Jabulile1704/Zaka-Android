package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoyaltyCardTest {
    @Test
    fun `symbology is detected from the number`() {
        assertEquals(BarcodeSymbology.EAN13, BarcodeSymbology.detected("6001087001618"))
        assertEquals(
            "a bad check digit is not EAN-13",
            BarcodeSymbology.CODE128,
            BarcodeSymbology.detected("6001087001610"),
        )
        assertEquals(BarcodeSymbology.CODE128, BarcodeSymbology.detected("ABC123"))
    }

    @Test
    fun acceptance() {
        assertTrue(BarcodeSymbology.EAN13.accepts("6001087001618"))
        assertFalse(BarcodeSymbology.EAN13.accepts("12345"))
        assertTrue(BarcodeSymbology.CODE128.accepts("ABC123"))
        assertFalse(BarcodeSymbology.CODE128.accepts(""))
    }

    @Test
    fun `last four`() {
        assertEquals("1618", CardNumber.lastFour("6001087001618"))
        assertEquals("12", CardNumber.lastFour("12"))
        assertEquals("", CardNumber.lastFour("ABC"))
    }

    @Test
    fun `a stored symbology survives the raw value round trip`() {
        assertEquals(BarcodeSymbology.EAN13, BarcodeSymbology.stored("ean13"))
        assertEquals(BarcodeSymbology.QR, BarcodeSymbology.stored("qr"))
        assertEquals(
            "an unreadable value must not crash the card list",
            BarcodeSymbology.CODE128,
            BarcodeSymbology.stored("nonsense"),
        )
    }
}
