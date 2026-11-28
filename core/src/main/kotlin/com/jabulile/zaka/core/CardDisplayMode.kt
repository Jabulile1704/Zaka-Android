package com.jabulile.zaka.core

/**
 * Whether a card is shown as a linear barcode or as a QR code of the same
 * number. A card opens in whichever form it was saved as, and only forms that
 * can actually draw the number are offered.
 */
enum class CardDisplayMode(val raw: String, val label: String) {
    BARCODE("barcode", "Barcode"),
    QR("qr", "QR code"),
    ;

    fun symbology(number: String, savedAs: BarcodeSymbology): BarcodeSymbology = when (this) {
        QR -> BarcodeSymbology.QR
        BARCODE ->
            if (savedAs == BarcodeSymbology.QR) BarcodeSymbology.detected(number) else savedAs
    }

    fun canDraw(number: String, savedAs: BarcodeSymbology): Boolean =
        symbology(number, savedAs).accepts(number)

    companion object {
        fun available(number: String, savedAs: BarcodeSymbology): List<CardDisplayMode> =
            entries.filter { it.canDraw(number, savedAs) }

        fun initial(number: String, savedAs: BarcodeSymbology): CardDisplayMode {
            val preferred = if (savedAs == BarcodeSymbology.QR) QR else BARCODE
            val offered = available(number, savedAs)
            return if (preferred in offered) preferred else offered.firstOrNull() ?: preferred
        }
    }
}
