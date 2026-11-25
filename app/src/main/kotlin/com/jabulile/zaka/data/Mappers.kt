package com.jabulile.zaka.data

import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.LoyaltyCard
import java.time.Instant

/*
 * The boundary between what the database holds and what the app reasons about.
 *
 * Reading goes through ExpenseCategory.stored() and BarcodeSymbology.stored(),
 * so a retired or unreadable value becomes something sensible in memory
 * without the row being rewritten. Reading must never write.
 */

internal fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = amount,
    category = ExpenseCategory.stored(categoryRaw),
    date = Instant.ofEpochMilli(dateEpochMillis),
    note = note,
)

internal fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    categoryRaw = category.raw,
    dateEpochMillis = date.toEpochMilli(),
    note = note,
)

internal fun LoyaltyCardEntity.toDomain(): LoyaltyCard = LoyaltyCard(
    id = id,
    name = name,
    number = number,
    symbology = BarcodeSymbology.stored(symbologyRaw),
    tintHex = tintHex,
    addedAt = Instant.ofEpochMilli(addedAtEpochMillis),
)

internal fun LoyaltyCard.toEntity(): LoyaltyCardEntity = LoyaltyCardEntity(
    id = id,
    name = name,
    number = number,
    symbologyRaw = symbology.raw,
    tintHex = tintHex,
    addedAtEpochMillis = addedAt.toEpochMilli(),
)
