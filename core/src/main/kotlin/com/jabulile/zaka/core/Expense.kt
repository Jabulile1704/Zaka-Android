package com.jabulile.zaka.core

import java.time.Instant

/**
 * One logged expense, as the app thinks about it.
 *
 * This is the domain type, not a database row: the Room entity maps onto it so
 * the logic in this module never depends on Android or on how the store is
 * shaped.
 */
data class Expense(
    val id: Long = 0,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Instant,
    val note: String = "",
) {
    val hasNote: Boolean get() = note.isNotBlank()

    /** What the list shows: the note when there is one, the category otherwise. */
    val title: String get() = if (hasNote) note else category.label
}
