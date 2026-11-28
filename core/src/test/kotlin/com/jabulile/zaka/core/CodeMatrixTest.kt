package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeMatrixTest {
    private val ean = "6001087001618"

    @Test
    fun `EAN-13 comes from the hand-written encoder`() {
        val code = CodeGenerator.of(ean, BarcodeSymbology.EAN13)
        assertTrue(code is CodeMatrix.Linear)
        assertEquals(Ean13.MODULE_COUNT, (code as CodeMatrix.Linear).width)
        assertEquals(Ean13.modules(ean), code.modules)
    }

    @Test
    fun `Code 128 is a start, some symbols, a checksum and a stop`() {
        val code = CodeGenerator.of("ABC123", BarcodeSymbology.CODE128) as CodeMatrix.Linear

        // Every Code 128 symbol is 11 modules; the stop pattern is 13.
        assertEquals("not a whole number of symbols", 0, (code.width - 13) % 11)
        assertTrue("a barcode starts with a bar", code.modules.first())
        assertTrue("and ends with one", code.modules.last())
    }

    @Test
    fun `a QR code is square, odd-sized, and has its finder patterns`() {
        val code = CodeGenerator.of(ean, BarcodeSymbology.QR) as CodeMatrix.Grid

        assertEquals(code.width, code.height)
        assertTrue("version 1 or larger", code.width >= 21)
        assertEquals("every QR size is odd", 1, code.width % 2)

        // The three finder patterns: a 7x7 ring with a 3x3 core. Without them
        // a reader cannot locate the code at all.
        for ((originX, originY) in listOf(0 to 0, code.width - 7 to 0, 0 to code.height - 7)) {
            assertTrue(
                "finder at $originX,$originY has a dark border",
                (0 until 7).all { code[originX + it, originY] && code[originX, originY + it] },
            )
            assertTrue(
                "finder at $originX,$originY has a light ring",
                !code[originX + 1, originY + 1] && !code[originX + 5, originY + 5],
            )
            assertTrue(
                "finder at $originX,$originY has a dark core",
                code[originX + 3, originY + 3],
            )
        }
    }

    @Test
    fun `a number a symbology cannot draw produces nothing`() {
        assertNull("bad check digit", CodeGenerator.of("6001087001610", BarcodeSymbology.EAN13))
        assertNull("too short", CodeGenerator.of("12345", BarcodeSymbology.EAN13))
        assertNull("not ASCII", CodeGenerator.of("KAART-é", BarcodeSymbology.CODE128))
        assertNull("empty", CodeGenerator.of("", BarcodeSymbology.QR))
        assertNull("empty", CodeGenerator.of("", BarcodeSymbology.CODE128))
    }

    @Test
    fun `QR carries what Code 128 will not`() {
        val awkward = "KAART-Ünïcode-42"
        assertNull(CodeGenerator.of(awkward, BarcodeSymbology.CODE128))
        assertTrue(
            "a QR encodes it as UTF-8",
            CodeGenerator.of(awkward, BarcodeSymbology.QR) is CodeMatrix.Grid,
        )
    }

    @Test
    fun `every code says how much space it needs around it`() {
        assertEquals(10, CodeGenerator.of(ean, BarcodeSymbology.EAN13)!!.quietZone)
        assertEquals(10, CodeGenerator.of("ABC123", BarcodeSymbology.CODE128)!!.quietZone)
        assertEquals(4, CodeGenerator.of(ean, BarcodeSymbology.QR)!!.quietZone)
    }

    @Test
    fun `the grid reads row-major`() {
        val code = CodeGenerator.of(ean, BarcodeSymbology.QR) as CodeMatrix.Grid
        assertEquals(code.width * code.height, code.modules.size)
        assertEquals(code[3, 2], code.modules[2 * code.width + 3])
    }
}
