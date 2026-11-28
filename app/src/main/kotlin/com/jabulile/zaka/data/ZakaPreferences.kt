package com.jabulile.zaka.data

import android.content.Context

/** Which way the insights breakdown is drawn. */
enum class BreakdownStyle(val raw: String) {
    DONUT("donut"),
    BARS("bars"),
    ;

    companion object {
        fun stored(raw: String?): BreakdownStyle =
            entries.firstOrNull { it.raw == raw } ?: DONUT
    }
}

/**
 * The handful of choices worth remembering between launches.
 *
 * `SharedPreferences` rather than DataStore: this is one string, read once when
 * the screen opens. DataStore would add a dependency and a coroutine to the
 * reading of a single enum.
 */
class ZakaPreferences(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var breakdownStyle: BreakdownStyle
        get() = BreakdownStyle.stored(preferences.getString(KEY_BREAKDOWN_STYLE, null))
        set(value) {
            preferences.edit().putString(KEY_BREAKDOWN_STYLE, value.raw).apply()
        }

    private companion object {
        const val FILE_NAME = "zaka"
        const val KEY_BREAKDOWN_STYLE = "insights.breakdown.style"
    }
}
