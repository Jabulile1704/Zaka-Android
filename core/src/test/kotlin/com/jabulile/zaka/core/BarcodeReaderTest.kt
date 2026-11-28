package com.jabulile.zaka.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Encode a code, draw it into a plane of luminance bytes the way a camera
 * would hand it over, and read it straight back.
 *
 * This is the scanner's whole decoding path, tested without a camera. What is
 * not tested here is the camera itself, which no test can supply.
 */
class BarcodeReaderTest {
    private val ean = "6001087001618"

    @Test
    fun `an EAN-13 read back off a frame`() {
        val frame = linearFrame(ean, BarcodeSymbology.EAN13)
        val scanned = BarcodeReader.read(frame.pixels, frame.width, frame.height)

        assertEquals(ean, scanned?.number)
        assertEquals(BarcodeSymbology.EAN13, scanned?.symbology)
    }

    @Test
    fun `a Code 128 read back off a frame`() {
        val frame = linearFrame("ZA-4471-9920", BarcodeSymbology.CODE128)
        val scanned = BarcodeReader.read(frame.pixels, frame.width, frame.height)

        assertEquals("ZA-4471-9920", scanned?.number)
        assertEquals(BarcodeSymbology.CODE128, scanned?.symbology)
    }

    @Test
    fun `a QR read back off a frame`() {
        val frame = squareFrame("KAART-Ünïcode-42")
        val scanned = BarcodeReader.read(frame.pixels, frame.width, frame.height)

        assertEquals("KAART-Ünïcode-42", scanned?.number)
        assertEquals(BarcodeSymbology.QR, scanned?.symbology)
    }

    @Test
    fun `padding at the end of each row does not corrupt the read`() {
        // Cameras hand over rows padded to an alignment boundary. Treating the
        // padding as image data is the classic way to make a good barcode
        // unreadable.
        val frame = linearFrame(ean, BarcodeSymbology.EAN13, rowPadding = 61)
        val scanned = BarcodeReader.read(
            frame.pixels,
            frame.width,
            frame.height,
            rowStride = frame.width + 61,
        )
        assertEquals(ean, scanned?.number)
    }

    @Test
    fun `a barcode lying sideways in the frame still reads`() {
        // What a camera actually hands over when a card is held horizontally
        // in front of a portrait phone.
        val frame = linearFrame(ean, BarcodeSymbology.EAN13)
        val sideways = turn(frame)

        assertNull(
            "and it genuinely cannot be read the other way up",
            BarcodeReader.read(sideways.pixels, sideways.width, sideways.height),
        )
        assertEquals(
            ean,
            BarcodeReader.readAnyOrientation(
                sideways.pixels,
                sideways.width,
                sideways.height,
            )?.number,
        )
    }

    @Test
    fun `reading any orientation still finds an upright code`() {
        val frame = linearFrame(ean, BarcodeSymbology.EAN13)
        assertEquals(
            ean,
            BarcodeReader.readAnyOrientation(frame.pixels, frame.width, frame.height)?.number,
        )
    }

    @Test
    fun `a frame with nothing in it reads as nothing`() {
        val blank = ByteArray(300 * 200) { WHITE }
        assertNull(BarcodeReader.read(blank, 300, 200))
    }

    @Test
    fun `noise is not mistaken for a code`() {
        val random = java.util.Random(42)
        val noise = ByteArray(300 * 200) { if (random.nextBoolean()) WHITE else BLACK }
        assertNull(BarcodeReader.read(noise, 300, 200))
    }

    @Test
    fun `a degenerate frame is refused rather than crashing`() {
        assertNull(BarcodeReader.read(ByteArray(0), 0, 0))
        assertNull("buffer smaller than the frame claims", BarcodeReader.read(ByteArray(10), 300, 200))
    }

    // --- drawing frames the way a camera would ------------------------------

    private class Frame(val pixels: ByteArray, val width: Int, val height: Int)

    /** A quarter turn anticlockwise, so readAnyOrientation has to undo it. */
    private fun turn(frame: Frame): Frame {
        val turned = ByteArray(frame.width * frame.height)
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                turned[(frame.width - 1 - x) * frame.height + y] = frame.pixels[y * frame.width + x]
            }
        }
        return Frame(turned, frame.height, frame.width)
    }

    private fun linearFrame(
        number: String,
        symbology: BarcodeSymbology,
        scale: Int = 3,
        height: Int = 120,
        rowPadding: Int = 0,
    ): Frame {
        val code = CodeGenerator.of(number, symbology) as CodeMatrix.Linear
        val quiet = code.quietZone
        val width = (code.width + quiet * 2) * scale
        val stride = width + rowPadding
        val pixels = ByteArray(stride * height) { WHITE }

        for (x in code.modules.indices) {
            if (!code.modules[x]) continue
            for (dx in 0 until scale) {
                val column = (quiet + x) * scale + dx
                for (y in 0 until height) pixels[y * stride + column] = BLACK
            }
        }
        return Frame(pixels, width, height)
    }

    private fun squareFrame(text: String, scale: Int = 6): Frame {
        val code = CodeGenerator.of(text, BarcodeSymbology.QR) as CodeMatrix.Grid
        val quiet = code.quietZone
        val side = (code.width + quiet * 2) * scale
        val pixels = ByteArray(side * side) { WHITE }

        for (y in 0 until code.height) {
            for (x in 0 until code.width) {
                if (!code[x, y]) continue
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        val px = (quiet + x) * scale + dx
                        val py = (quiet + y) * scale + dy
                        pixels[py * side + px] = BLACK
                    }
                }
            }
        }
        return Frame(pixels, side, side)
    }

    private companion object {
        const val WHITE: Byte = -1 // 0xFF
        const val BLACK: Byte = 0
    }
}
