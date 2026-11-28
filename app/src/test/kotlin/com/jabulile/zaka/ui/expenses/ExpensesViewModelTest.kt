package com.jabulile.zaka.ui.expenses

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.ZakaDatabase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The state holder over the real database. Nothing here is mocked: a fake
 * repository would only prove that the fake behaves like the fake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExpensesViewModelTest {
    private val zone: ZoneId = ZoneId.of("Africa/Johannesburg")

    private lateinit var database: ZakaDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: ExpensesViewModel
    private lateinit var clock: MovableClock

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExpenseRepository(database.expenses(), zone)
        clock = MovableClock(Instant.now(), zone)
        viewModel = ExpensesViewModel(repository, zone, clock)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the days and the month total arrive in one state`() = runTest {
        repository.save(expense(100.0, ExpenseCategory.TAKEAWAYS))
        repository.save(expense(50.0, ExpenseCategory.TRANSPORT))

        val state = viewModel.state.filter { it.loaded }.first()

        assertEquals(1, state.days.size)
        assertEquals(2, state.days.single().expenses.size)
        assertEquals(150.0, state.days.single().total, 1e-9)
        assertEquals(150.0, state.monthTotal, 1e-9)
    }

    @Test
    fun `an empty store still produces a state`() = runTest {
        val state = viewModel.state.filter { it.loaded }.first()
        assertEquals(0, state.days.size)
        assertEquals(0.0, state.monthTotal, 1e-9)
    }

    @Test
    fun `deleting an expense drops it from the state`() = runTest {
        val id = repository.save(expense(100.0, ExpenseCategory.RENT))
        assertEquals(1, viewModel.state.filter { it.loaded }.first().days.size)

        viewModel.delete(id)

        assertEquals(
            0,
            viewModel.state.filter { it.loaded && it.days.isEmpty() }.first().days.size,
        )
    }

    @Test
    fun `exporting offers every expense, oldest first, and then clears`() = runTest {
        repository.save(expense(100.0, ExpenseCategory.RENT, Instant.parse("2026-11-21T10:00:00Z")))
        repository.save(expense(40.0, ExpenseCategory.WIFI, Instant.parse("2026-11-23T10:00:00Z")))
        viewModel.state.filter { it.loaded }.first()

        viewModel.export()

        val csv = viewModel.state.filter { it.export != null }.first().export!!
        val lines = csv.text.trimEnd('\n').split("\n")
        assertEquals("Date,Category,Note,Amount", lines[0])
        assertEquals(3, lines.size)
        assertTrue("oldest first", lines[1].contains("100.00"))
        assertTrue(lines[2].contains("40.00"))
        assertTrue(csv.name.startsWith("Zaka-expenses-"))

        viewModel.exportShared()
        assertNull(
            "the share sheet must not reopen on the next recomposition",
            viewModel.state.filter { it.export == null }.first().export,
        )
    }

    @Test
    fun `the month total follows the calendar, not the view model's birthday`() = runTest {
        // This view model's scope must share the test's scheduler, so that
        // advancing virtual time below actually times the sharing out.
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

        val november = LocalDate.of(2026, 11, 20).atTime(12, 0).atZone(zone).toInstant()
        val december = LocalDate.of(2026, 12, 2).atTime(12, 0).atZone(zone).toInstant()
        clock.now = LocalDate.of(2026, 11, 30).atTime(23, 59).atZone(zone).toInstant()

        repository.save(expense(100.0, ExpenseCategory.RENT, november))
        repository.save(expense(40.0, ExpenseCategory.WIFI, december))

        val model = ExpensesViewModel(repository, zone, clock)
        assertEquals(100.0, model.state.filter { it.loaded }.first().monthTotal, 1e-9)

        // Let the sharing time out, as it does when the screen stops being
        // observed, then push the clock over midnight and look again.
        advanceTimeBy(6_000)
        clock.now = LocalDate.of(2026, 12, 1).atTime(0, 1).atZone(zone).toInstant()

        val rolled = model.state.filter { it.monthTotal == 40.0 }.first()
        assertEquals(
            "the day headings follow the same read",
            LocalDate.of(2026, 12, 1),
            rolled.today,
        )
    }

    private fun expense(amount: Double, category: ExpenseCategory) = Expense(
        amount = amount,
        category = category,
        // Now, because the month total only counts the current month.
        date = Instant.now(),
    )

    private fun expense(amount: Double, category: ExpenseCategory, at: Instant) =
        Expense(amount = amount, category = category, date = at)

    /** A clock the test can push over midnight. */
    private class MovableClock(var now: Instant, private val where: ZoneId) : Clock() {
        override fun getZone(): ZoneId = where

        override fun withZone(other: ZoneId): Clock = MovableClock(now, other)

        override fun instant(): Instant = now
    }
}
