package com.jabulile.zaka.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The real Room database, in memory, on the JVM.
 *
 * Robolectric rather than an emulator: these assertions are about SQL and
 * mapping, not about a device, and an emulator job would turn a three-second
 * suite into a five-minute one. The iOS equivalent used an in-memory
 * SwiftData container for the same reason.
 */
@RunWith(RobolectricTestRunner::class)
class ExpenseStoreTest {
    private val johannesburg: ZoneId = ZoneId.of("Africa/Johannesburg")

    private lateinit var database: ZakaDatabase
    private lateinit var expenses: ExpenseRepository

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        expenses = ExpenseRepository(database.expenses(), johannesburg)
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `an expense round trips through the store`() = runTest {
        val id = expenses.save(
            Expense(
                amount = 249.90,
                category = ExpenseCategory.TRANSPORT,
                date = Instant.parse("2026-11-22T17:45:00Z"),
                note = "Uber home",
            ),
        )

        val saved = expenses.find(id)
        assertNotNull(saved)
        assertEquals(249.90, saved!!.amount, 1e-9)
        assertEquals(ExpenseCategory.TRANSPORT, saved.category)
        assertEquals("Uber home", saved.note)
        assertEquals(Instant.parse("2026-11-22T17:45:00Z"), saved.date)
        assertEquals(id, saved.id)
    }

    @Test
    fun `a retired category is read through the mapping and the row is left alone`() = runTest {
        val id = database.expenses().insert(
            ExpenseEntity(
                amount = 10.0,
                categoryRaw = "food",
                dateEpochMillis = 0,
                note = "",
            ),
        )

        assertEquals(ExpenseCategory.TAKEAWAYS, expenses.find(id)!!.category)
        assertEquals(
            "reading must not rewrite the store",
            "food",
            database.expenses().find(id)!!.categoryRaw,
        )
    }

    @Test
    fun `days come back grouped, totalled and newest first`() = runTest {
        val today = LocalDate.now(johannesburg)
        val noonToday = today.atTime(12, 0).atZone(johannesburg).toInstant()
        val noonYesterday = today.minusDays(1).atTime(12, 0).atZone(johannesburg).toInstant()

        expenses.save(expense(100.0, ExpenseCategory.TAKEAWAYS, noonToday))
        expenses.save(expense(50.0, ExpenseCategory.TRANSPORT, noonToday))
        expenses.save(expense(25.0, ExpenseCategory.AIRTIME, noonYesterday))

        val days = expenses.observeDays().first()

        assertEquals(2, days.size)
        assertEquals("Today", days[0].title(today))
        assertEquals(150.0, days[0].total, 1e-9)
        assertEquals("Yesterday", days[1].title(today))
        assertEquals(25.0, days[1].total, 1e-9)
    }

    @Test
    fun `saving an expense that has an id updates it instead of adding another`() = runTest {
        val id = expenses.save(expense(100.0, ExpenseCategory.RENT, Instant.EPOCH))
        val again = expenses.save(
            expenses.find(id)!!.copy(amount = 120.0, note = "with the water bill"),
        )

        assertEquals("the same row", id, again)
        assertEquals(1, expenses.observeDays().first().single().expenses.size)
        assertEquals(120.0, expenses.find(id)!!.amount, 1e-9)
        assertEquals("with the water bill", expenses.find(id)!!.note)
    }

    @Test
    fun `deleting removes the row`() = runTest {
        val id = expenses.save(expense(100.0, ExpenseCategory.RENT, Instant.EPOCH))
        expenses.delete(id)
        assertNull(expenses.find(id))
        assertEquals(0, expenses.observeDays().first().size)
    }

    @Test
    fun `a month holds the expenses that fall inside it locally, and no others`() = runTest {
        val november = YearMonth.of(2026, 11)

        // 21:59:59.999Z on 31 October is still October in Johannesburg; one
        // millisecond later is November.
        expenses.save(expense(10.0, ExpenseCategory.RENT, Instant.parse("2026-10-31T21:59:59.999Z")))
        expenses.save(expense(20.0, ExpenseCategory.RENT, Instant.parse("2026-10-31T22:00:00Z")))
        expenses.save(expense(30.0, ExpenseCategory.WIFI, Instant.parse("2026-11-30T21:59:59.999Z")))
        expenses.save(expense(40.0, ExpenseCategory.WIFI, Instant.parse("2026-11-30T22:00:00Z")))

        assertEquals(
            listOf(30.0, 20.0),
            expenses.observeMonth(november).first().map { it.amount },
        )
        assertEquals(50.0, expenses.observeMonthTotal(november).first(), 1e-9)
    }

    @Test
    fun `an empty month totals zero rather than nothing`() = runTest {
        assertEquals(0.0, expenses.observeMonthTotal(YearMonth.of(2026, 1)).first(), 1e-9)
    }

    @Test
    fun `a month breaks down per category`() = runTest {
        val november = YearMonth.of(2026, 11)
        val inside = Instant.parse("2026-11-10T08:00:00Z")

        expenses.save(expense(100.0, ExpenseCategory.GROCERIES, inside))
        expenses.save(expense(50.0, ExpenseCategory.GROCERIES, inside))
        expenses.save(expense(25.0, ExpenseCategory.AIRTIME, inside))

        val totals = expenses.observeMonthBreakdown(november).first()
        assertEquals(150.0, totals[ExpenseCategory.GROCERIES]!!, 1e-9)
        assertEquals(25.0, totals[ExpenseCategory.AIRTIME]!!, 1e-9)
    }

    private fun expense(amount: Double, category: ExpenseCategory, date: Instant) =
        Expense(amount = amount, category = category, date = date)
}
