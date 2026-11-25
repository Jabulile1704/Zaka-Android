package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDisplayModeTest {
    private val ean = "6001087001618"

    @Test
    fun `a barcode card offers both and opens as a barcode`() {
        assertEquals(
            listOf(CardDisplayMode.BARCODE, CardDisplayMode.QR),
            CardDisplayMode.available(ean, BarcodeSymbology.EAN13),
        )
        assertEquals(
            CardDisplayMode.BARCODE,
            CardDisplayMode.initial(ean, BarcodeSymbology.EAN13),
        )
    }

    @Test
    fun `barcode mode keeps the saved linear type`() {
        assertEquals(
            BarcodeSymbology.EAN13,
            CardDisplayMode.BARCODE.symbology(ean, BarcodeSymbology.EAN13),
        )
        assertEquals(
            "a card deliberately saved as Code 128 must not be promoted to EAN-13",
            BarcodeSymbology.CODE128,
            CardDisplayMode.BARCODE.symbology(ean, BarcodeSymbology.CODE128),
        )
    }

    @Test
    fun `QR mode is always QR`() {
        assertEquals(
            BarcodeSymbology.QR,
            CardDisplayMode.QR.symbology(ean, BarcodeSymbology.EAN13),
        )
        assertEquals(
            BarcodeSymbology.QR,
            CardDisplayMode.QR.symbology("ABC123", BarcodeSymbology.CODE128),
        )
    }

    @Test
    fun `a card saved as QR opens as QR and infers a linear type`() {
        assertEquals(CardDisplayMode.QR, CardDisplayMode.initial(ean, BarcodeSymbology.QR))
        assertEquals(
            BarcodeSymbology.EAN13,
            CardDisplayMode.BARCODE.symbology(ean, BarcodeSymbology.QR),
        )
        assertEquals(
            BarcodeSymbology.CODE128,
            CardDisplayMode.BARCODE.symbology("ABC123", BarcodeSymbology.QR),
        )
    }

    @Test
    fun `a number that cannot be a linear barcode offers only QR`() {
        val number = "KAART-Unicode-42é"
        assertEquals(
            listOf(CardDisplayMode.QR),
            CardDisplayMode.available(number, BarcodeSymbology.QR),
        )
        assertEquals(CardDisplayMode.QR, CardDisplayMode.initial(number, BarcodeSymbology.QR))
    }

    @Test
    fun `a broken EAN falls back to what can be drawn`() {
        val bad = "6001087001610"
        assertEquals(
            "an invalid EAN-13 cannot be drawn as EAN-13, so only QR is offered",
            listOf(CardDisplayMode.QR),
            CardDisplayMode.available(bad, BarcodeSymbology.EAN13),
        )
        assertEquals(CardDisplayMode.QR, CardDisplayMode.initial(bad, BarcodeSymbology.EAN13))
    }

    @Test
    fun `an empty number offers nothing`() {
        assertTrue(CardDisplayMode.available("", BarcodeSymbology.CODE128).isEmpty())
    }
}
