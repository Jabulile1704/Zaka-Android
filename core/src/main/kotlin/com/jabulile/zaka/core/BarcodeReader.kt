package com.jabulile.zaka.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** What a camera frame turned out to contain. */
data class Scanned(
    val number: String,
    /** Null when the code is a kind Zaka does not store. */
    val symbology: BarcodeSymbology?,
)

/**
 * Reads a barcode out of one camera frame.
 *
 * The frame arrives as a plane of luminance bytes — one per pixel, which is
 * exactly what the Y plane of a camera's YUV output already is, so nothing has
 * to be converted or copied into a bitmap to scan it.
 *
 * Pure: no Android, no camera, no lifecycle. The scanner screen's only job is
 * to hand frames to this and show what comes back, which is what lets the
 * decoding be tested by encoding a code and reading it straight back.
 */
object BarcodeReader {
    private val FORMATS = listOf(
        BarcodeFormat.EAN_13,
        BarcodeFormat.CODE_128,
        BarcodeFormat.QR_CODE,
        // Read, but not stored as such: an EAN-8 or a UPC-A still gives a
        // number worth putting in the field.
        BarcodeFormat.EAN_8,
        BarcodeFormat.UPC_A,
    )

    /**
     * Null when the frame holds no readable code, which is most frames — this
     * is called on every one the camera produces.
     *
     * [rowStride] is the number of bytes per row the camera actually used,
     * which is often wider than [width] because of alignment padding. Passing
     * the padding in as image data turns a perfectly good barcode into noise.
     */
    fun read(
        luminance: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
    ): Scanned? {
        if (width <= 0 || height <= 0) return null
        if (luminance.size < rowStride * height) return null

        val source = PlanarYUVLuminanceSource(
            luminance,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false,
        )

        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to FORMATS,
                    DecodeHintType.TRY_HARDER to true,
                ),
            )
        }

        return try {
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            val text = result.text?.takeIf { it.isNotBlank() } ?: return null
            Scanned(number = text, symbology = symbologyOf(result.barcodeFormat))
        } catch (notFound: Exception) {
            // ZXing signals "no code in this frame" by throwing, and that is
            // the ordinary case here rather than an error worth reporting.
            null
        }
    }

    /**
     * The same read, tried both ways round.
     *
     * A camera's rows run along the sensor's long axis, so a barcode held
     * horizontally in front of a portrait phone arrives sideways. ZXing cannot
     * rotate this kind of source itself, so a frame that fails is rotated a
     * quarter turn and tried again. Without this the scanner simply never
     * reads anything, which is the single most common way a barcode scanner
     * is broken.
     */
    fun readAnyOrientation(
        luminance: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
    ): Scanned? {
        read(luminance, width, height, rowStride)?.let { return it }

        val turned = rotateQuarterTurn(luminance, width, height, rowStride) ?: return null
        return read(turned, height, width, height)
    }

    /** Clockwise, into a tightly packed plane of [height] x [width]. */
    private fun rotateQuarterTurn(
        luminance: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
    ): ByteArray? {
        if (luminance.size < rowStride * height) return null
        val turned = ByteArray(width * height)
        for (y in 0 until height) {
            val row = y * rowStride
            val column = height - 1 - y
            for (x in 0 until width) {
                turned[x * height + column] = luminance[row + x]
            }
        }
        return turned
    }

    private fun symbologyOf(format: BarcodeFormat?): BarcodeSymbology? = when (format) {
        BarcodeFormat.EAN_13 -> BarcodeSymbology.EAN13
        BarcodeFormat.CODE_128 -> BarcodeSymbology.CODE128
        BarcodeFormat.QR_CODE -> BarcodeSymbology.QR
        else -> null
    }
}
