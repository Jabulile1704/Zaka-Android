package com.jabulile.zaka.ui.insights

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.data.BreakdownStyle
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.ZakaDatabase
import com.jabulile.zaka.data.ZakaPreferences
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InsightsViewModelTest {
    private val zone: ZoneId = ZoneId.of("Africa/Johannesburg")

    /**
     * A fixed "now". With the system clock these assertions depended on which
     * month the suite happened to run in, and on "now" being read identically
     * in five places.
     */
    private val today: LocalDate = LocalDate.of(2026, 11, 25)
    private val clock: Clock = Clock.fixed(
        today.atTime(12, 0).atZone(zone).toInstant(),
        zone,
    )

    private lateinit var database: ZakaDatabase
    private lateinit var expenses: ExpenseRepository
    private lateinit var preferences: ZakaPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZakaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenses = ExpenseRepository(database.expenses(), zone)
        preferences = ZakaPreferences(context)
        preferences.breakdownStyle = BreakdownStyle.DONUT
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `this month totals what was logged in it`() = runTest {
        expenses.save(expense(100.0, ExpenseCategory.GROCERIES, today))
        expenses.save(expense(50.0, ExpenseCategory.GROCERIES, today))
        expenses.save(expense(25.0, ExpenseCategory.AIRTIME, today))

        val state = viewModel().state.filter { it.loaded }.first()

        assertEquals(175.0, state.total, 1e-9)
        assertEquals(
            listOf(ExpenseCategory.GROCERIES, ExpenseCategory.AIRTIME),
            state.slices.map { it.category },
        )
    }

    @Test
    fun `the daily rate counts the days that have happened, not the whole month`() = runTest {
        expenses.save(expense(100.0, ExpenseCategory.RENT, today))

        val state = viewModel().state.filter { it.loaded }.first()

        assertEquals(100.0 / today.dayOfMonth, state.perDay, 1e-9)
    }

    @Test
    fun `stepping back shows another month and cannot step past this one`() = runTest {
        val lastMonth = today.minusMonths(1).withDayOfMonth(1)
        expenses.save(expense(100.0, ExpenseCategory.RENT, today))
        expenses.save(expense(40.0, ExpenseCategory.WIFI, lastMonth))

        val model = viewModel()
        assertFalse("already at this month", model.state.filter { it.loaded }.first().canGoForward)

        model.previousMonth()
        val previous = model.state
            .filter { it.loaded && it.month == YearMonth.from(lastMonth) }
            .first()
        assertEquals(40.0, previous.total, 1e-9)
        assertTrue("October can step forward to November", previous.canGoForward)

        model.nextMonth()
        val back = model.state
            .filter { it.loaded && it.month == YearMonth.from(today) }
            .first()
        assertEquals(100.0, back.total, 1e-9)

        model.nextMonth()
        assertEquals(
            "the future holds no expenses",
            YearMonth.from(today),
            model.state.value.month,
        )
    }

    @Test
    fun `a month with nothing in it has no slices and no NaN`() = runTest {
        val state = viewModel().state.filter { it.loaded }.first()
        assertEquals(0.0, state.total, 1e-9)
        assertEquals(0.0, state.perDay, 1e-9)
        assertTrue(state.slices.isEmpty())
    }

    @Test
    fun `the chosen style is remembered`() = runTest {
        val model = viewModel()
        model.styleChosen(BreakdownStyle.BARS)

        assertEquals(BreakdownStyle.BARS, model.state.filter { it.loaded }.first().style)
        assertEquals(
            "and it survives the screen going away",
            BreakdownStyle.BARS,
            viewModel().state.filter { it.loaded }.first().style,
        )
    }

    @Test
    fun `a month's figures always belong to the month being shown`() = runTest {
        val lastMonth = today.minusMonths(1).withDayOfMonth(1)
        expenses.save(expense(100.0, ExpenseCategory.RENT, today))
        expenses.save(expense(40.0, ExpenseCategory.WIFI, lastMonth))

        val model = viewModel()
        val seen = mutableListOf<Pair<java.time.YearMonth, Double>>()

        model.state.filter { it.loaded }.first().let { seen += it.month to it.total }
        model.previousMonth()
        seen += model.state.filter { it.month == YearMonth.from(lastMonth) }.first()
            .let { it.month to it.total }

        // Every state the screen could have rendered pairs a month with its
        // own total, never one month's heading over another month's figures.
        for ((month, total) in seen) {
            val expected = if (month == YearMonth.from(today)) 100.0 else 40.0
            assertEquals("$month showed the wrong total", expected, total, 1e-9)
        }
    }

    private fun viewModel() = InsightsViewModel(expenses, preferences, zone, clock)

    private fun expense(amount: Double, category: ExpenseCategory, on: LocalDate): Expense {
        val moment: Instant = on.atTime(12, 0).atZone(zone).toInstant()
        return Expense(amount = amount, category = category, date = moment)
    }
}
