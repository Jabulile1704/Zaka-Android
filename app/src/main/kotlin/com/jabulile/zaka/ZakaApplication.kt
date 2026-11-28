package com.jabulile.zaka

import android.app.Application
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.LoyaltyCardRepository
import com.jabulile.zaka.data.ZakaDatabase

/**
 * Where the one database and the two repositories live.
 *
 * This is the whole of Zaka's dependency injection. Two repositories over one
 * database, constructed once and handed down the composable tree, is cheaper
 * to read than a graph — see [docs/ARCHITECTURE.md].
 */
class ZakaApplication : Application() {
    private val database: ZakaDatabase by lazy { ZakaDatabase.get(this) }

    val expenses: ExpenseRepository by lazy { ExpenseRepository(database.expenses()) }

    val cards: LoyaltyCardRepository by lazy { LoyaltyCardRepository(database.cards()) }
}
