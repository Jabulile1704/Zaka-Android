package com.jabulile.zaka.core

/** How a loyalty card number is drawn. [raw] is what the database stores. */
enum class BarcodeSymbology(val raw: String, val label: String, val hint: String) {
    EAN13("ean13", "EAN-13", "13 digits, the usual retail barcode"),
    CODE128("code128", "Code 128", "Letters and digits"),
    QR("qr", "QR", "Square code"),
    ;

    /** Whether this symbology can actually draw [number]. */
    fun accepts(number: String): Boolean = when (this) {
        EAN13 -> Ean13.isValid(number)
        CODE128 -> number.isNotEmpty() && number.all { it.code < 128 }
        QR -> number.isNotEmpty()
    }

    companion object {
        private val byRaw: Map<String, BarcodeSymbology> = entries.associateBy { it.raw }

        fun stored(raw: String): BarcodeSymbology = byRaw[raw] ?: CODE128

        /** Picks the linear symbology that suits [number] as it is typed. */
        fun detected(number: String): BarcodeSymbology =
            if (Ean13.isValid(number)) EAN13 else CODE128
    }
}
