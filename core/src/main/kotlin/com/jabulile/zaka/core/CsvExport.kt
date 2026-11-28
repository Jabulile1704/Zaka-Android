package com.jabulile.zaka.core

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A file ready to be handed to whatever the platform uses for sharing. */
data class CsvFile(
    val name: String,
    val text: String,
)

/**
 * Expenses and cards as CSV.
 *
 * Byte-for-byte the same shape as the iOS export for the same data, so the two
 * can be compared or concatenated: the same columns, the same quoting, the
 * same `yyyy-MM-dd HH:mm` stamps, the same two-decimal amounts, newline
 * endings and a trailing newline.
 *
 * The export is for machines. Amounts carry no currency symbol and no
 * thousands separator, because `R1,234.56` imports into a spreadsheet as text
 * that cannot be summed — which is the whole reason the display format and
 * this one are allowed to differ.
 */
object CsvExport {
    const val EXPENSE_HEADER: String = "Date,Category,Note,Amount"

    /** Cards are not in the iOS export at all; these columns are new here. */
    const val CARD_HEADER: String = "Shop,Number,Type,Added"

    fun expenses(
        expenses: List<Expense>,
        zone: ZoneId = ZoneId.systemDefault(),
        on: LocalDate = LocalDate.now(zone),
    ): CsvFile {
        val stamp = stamps(zone)
        val rows = expenses
            .sortedBy { it.date }
            .map { expense ->
                listOf(
                    quoted(stamp.format(expense.date)),
                    quoted(expense.category.label),
                    quoted(expense.note),
                    amount(expense.amount),
                ).joinToString(",")
            }
        return CsvFile(
            name = "Zaka-expenses-${on.format(FILE_DATE)}.csv",
            text = document(EXPENSE_HEADER, rows),
        )
    }

    fun cards(
        cards: List<LoyaltyCard>,
        zone: ZoneId = ZoneId.systemDefault(),
        on: LocalDate = LocalDate.now(zone),
    ): CsvFile {
        val stamp = stamps(zone)
        val rows = cards
            .sortedBy { it.addedAt }
            .map { card ->
                listOf(
                    quoted(card.name),
                    quoted(card.number),
                    quoted(card.symbology.label),
                    quoted(stamp.format(card.addedAt)),
                ).joinToString(",")
            }
        return CsvFile(
            name = "Zaka-cards-${on.format(FILE_DATE)}.csv",
            text = document(CARD_HEADER, rows),
        )
    }

    private fun document(header: String, rows: List<String>): String =
        (listOf(header) + rows).joinToString("\n") + "\n"

    /**
     * Always quoted, with embedded quotes doubled. Quoting only the fields
     * that need it would be valid CSV too, but then whether a row is quoted
     * would depend on what somebody typed in a note, and the two exports would
     * stop being comparable.
     */
    private fun quoted(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    /**
     * `Locale.US`, not the device's.
     *
     * On a phone set to a comma-decimal locale — which is most of South
     * Africa's — the default would format 45.50 as `45,50`, and a CSV would
     * silently gain a column.
     */
    private fun amount(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun stamps(zone: ZoneId): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US).withZone(zone)

    private val FILE_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
}
