package com.jabulile.zaka.core

import com.google.zxing.EncodeHintType
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * A code as modules, before anything has decided how big to draw it.
 *
 * Deliberately not a bitmap. A scaled bitmap barcode blurs at the module edges
 * and scans badly, so what leaves this module is the pattern itself and the
 * view draws it as rectangles at whatever size the screen gives it.
 */
sealed interface CodeMatrix {
    /** How many light modules must surround the code for a scanner to read it. */
    val quietZone: Int

    /** A one-dimensional barcode: one row of modules, left to right. */
    data class Linear(
        val modules: List<Boolean>,
        override val quietZone: Int,
    ) : CodeMatrix {
        val width: Int get() = modules.size
    }

    /** A two-dimensional code: [width] × [height] modules, row-major. */
    data class Grid(
        val width: Int,
        val height: Int,
        val modules: List<Boolean>,
        override val quietZone: Int,
    ) : CodeMatrix {
        operator fun get(x: Int, y: Int): Boolean = modules[y * width + x]
    }
}

/**
 * Turns a card number into the modules that draw it.
 *
 * EAN-13 is [Ean13], written by hand because most retail loyalty cards use it
 * and rendering those digits as Code 128 produces a barcode the till misreads.
 * Code 128 and QR come from ZXing, which is the same division the iOS app
 * makes with Core Image: use the library for what it has, write what it lacks.
 */
object CodeGenerator {
    /** Null when [symbology] cannot draw [number] at all. */
    fun of(number: String, symbology: BarcodeSymbology): CodeMatrix? {
        if (!symbology.accepts(number)) return null
        return when (symbology) {
            BarcodeSymbology.EAN13 ->
                Ean13.modules(number)?.let { CodeMatrix.Linear(it, symbology.quietZone) }

            BarcodeSymbology.CODE128 -> code128(number, symbology.quietZone)
            BarcodeSymbology.QR -> qr(number, symbology.quietZone)
        }
    }

    private fun code128(number: String, quietZone: Int): CodeMatrix.Linear? = runCatching {
        CodeMatrix.Linear(Code128Writer().encode(number).map { it }, quietZone)
    }.getOrNull()

    private fun qr(text: String, quietZone: Int): CodeMatrix.Grid? = runCatching {
        // UTF-8 explicitly. Left to the default, a card number containing a
        // non-ASCII character encodes as something a reader will not give back.
        val code = Encoder.encode(
            text,
            ErrorCorrectionLevel.M,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
        val matrix = code.matrix ?: return null
        CodeMatrix.Grid(
            width = matrix.width,
            height = matrix.height,
            modules = buildList(matrix.width * matrix.height) {
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        add(matrix.get(x, y).toInt() == 1)
                    }
                }
            },
            quietZone = quietZone,
        )
    }.getOrNull()
}
