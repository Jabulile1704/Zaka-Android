package com.jabulile.zaka.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An expense as the database holds it.
 *
 * Deliberately not the domain record. The category is a raw string, never an
 * enum, so renaming or retiring a category in Kotlin cannot change what rows
 * already in the store mean — the same decision as on iOS, and the one that
 * saved that app twice. The date is epoch milliseconds for the same reason:
 * a plain integer that sorts correctly in SQL and needs no converter.
 */
@Entity(
    tableName = "expenses",
    indices = [Index("date")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    @ColumnInfo(name = "category")
    val categoryRaw: String,
    @ColumnInfo(name = "date")
    val dateEpochMillis: Long,
    val note: String,
)
