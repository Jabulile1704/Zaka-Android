package com.jabulile.zaka.core

/**
 * The sixteen categories an expense can have.
 *
 * [raw] is what the database stores, never the enum name, so renaming or
 * retiring a category cannot silently re-map rows that are already saved.
 * [iconName] is a Material Symbols token and [colorHex] an sRGB triple; both
 * are data rather than drawing, which keeps this module free of Android.
 */
enum class ExpenseCategory(
    val raw: String,
    val label: String,
    val iconName: String,
    val colorHex: String,
) {
    TAKEAWAYS("takeaways", "Takeaways", "takeout_dining", "E8712A"),
    GROCERIES("groceries", "Groceries", "shopping_basket", "2E9E4F"),
    TRANSPORT("transport", "Transport", "directions_car", "2B6BE8"),
    RENT("rent", "Rent", "home", "8D6E4A"),
    UTILITIES("utilities", "Utilities", "bolt", "F2A93B"),
    HEALTH("health", "Health", "medical_services", "D93A36"),
    WIFI("wifi", "WiFi", "wifi", "2AAEC4"),
    SUBSCRIPTIONS("subscriptions", "Subscriptions", "autorenew", "7C4DD1"),
    SHOPPING("shopping", "Shopping", "shopping_bag", "D94F8A"),
    ACCOUNTS("accounts", "Accounts", "credit_card", "4A51C4"),
    CASH("cash", "Cash", "payments", "35C08A"),
    TRAVELLING("travelling", "Travelling", "flight", "2A9C9C"),
    DATES("dates", "Dates", "favorite", "B3246B"),
    AIRTIME("airtime", "Airtime", "cell_tower", "0E8A6B"),
    HAIRCUT("haircut", "Haircut", "content_cut", "D9A520"),
    OTHERS("other", "Others", "grid_view", "7A7A80"),
    ;

    companion object {
        private val byRaw: Map<String, ExpenseCategory> = entries.associateBy { it.raw }

        /** Categories that existed once and no longer do. */
        private val retired: Map<String, ExpenseCategory> = mapOf(
            "food" to TAKEAWAYS,
            "bills" to ACCOUNTS,
            "fun" to OTHERS,
        )

        /** Reads a stored value. Anything unrecognised falls back to [OTHERS]. */
        fun stored(raw: String): ExpenseCategory = byRaw[raw] ?: retired[raw] ?: OTHERS
    }
}
