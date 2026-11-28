package com.jabulile.zaka.ui.form

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.ZakaDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExpenseFormViewModelTest {
    private val zone: ZoneId = ZoneId.of("Africa/Johannesburg")

    private lateinit var database: ZakaDatabase
    private lateinit var expenses: ExpenseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        expenses = ExpenseRepository(database.expenses(), zone)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new expense is saved with what was typed`() = runTest {
        val form = adding()
        form.amountChanged("249.90")
        form.categoryChosen(ExpenseCategory.TRANSPORT)
        form.noteChanged("  Uber home  ")
        form.dateChosen(LocalDate.of(2026, 11, 20))
        form.saveAndSettle()

        val saved = expenses.observeDays().first().single().expenses.single()
        assertEquals(249.90, saved.amount, 1e-9)
        assertEquals(ExpenseCategory.TRANSPORT, saved.category)
        assertEquals("the note is trimmed", "Uber home", saved.note)
        assertEquals(LocalDate.of(2026, 11, 20), saved.date.atZone(zone).toLocalDate())
    }

    @Test
    fun `a comma is the decimal point here too`() = runTest {
        val form = adding()
        form.amountChanged("45,50")
        form.saveAndSettle()

        assertEquals(
            45.50,
            expenses.observeDays().first().single().expenses.single().amount,
            1e-9,
        )
    }

    @Test
    fun `nothing worth saving cannot be saved`() = runTest {
        val form = adding()
        assertFalse("an empty field", form.state.value.canSave)

        form.amountChanged("abc")
        assertFalse("not a number", form.state.value.canSave)

        form.amountChanged("0")
        assertFalse("a zero expense is not an expense", form.state.value.canSave)

        form.amountChanged("0,01")
        assertTrue(form.state.value.canSave)
    }

    @Test
    fun `saving nothing writes nothing`() = runTest {
        val form = adding()
        form.amountChanged("")
        form.save()
        assertEquals(0, expenses.observeDays().first().size)
        assertFalse("the form stays open", form.state.value.finished)
    }

    @Test
    fun `editing loads the expense into the form`() = runTest {
        val id = expenses.save(
            Expense(
                amount = 3700.0,
                category = ExpenseCategory.RENT,
                date = Instant.parse("2026-11-01T06:30:00Z"),
                note = "November",
            ),
        )

        val state = editing(id).state.filter { it.loaded }.first()

        assertEquals("3700.00", state.amountText)
        assertEquals(ExpenseCategory.RENT, state.category)
        assertEquals("November", state.note)
        assertEquals(LocalDate.of(2026, 11, 1), state.date)
        assertTrue(state.editing)
    }

    @Test
    fun `editing keeps the time of day, so the row does not move`() = runTest {
        val original = Instant.parse("2026-11-01T06:30:00Z")
        val id = expenses.save(
            Expense(amount = 100.0, category = ExpenseCategory.RENT, date = original),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        form.amountChanged("120")
        form.saveAndSettle()

        assertEquals(original, expenses.find(id)!!.date)
    }

    @Test
    fun `updating does not add a second row`() = runTest {
        val id = expenses.save(
            Expense(amount = 100.0, category = ExpenseCategory.RENT, date = Instant.now()),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        form.amountChanged("120")
        form.saveAndSettle()

        val all = expenses.observeDays().first().flatMap { it.expenses }
        assertEquals(1, all.size)
        assertEquals(120.0, all.single().amount, 1e-9)
    }

    @Test
    fun `deleting from the form removes the expense`() = runTest {
        val id = expenses.save(
            Expense(amount = 100.0, category = ExpenseCategory.RENT, date = Instant.now()),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        form.delete()

        // delete() launches and returns; the row is gone once the form says
        // it has finished. Asserting straight away raced the database and
        // passed only on a machine quick enough to hide it.
        form.state.filter { it.finished }.first()

        assertNull(expenses.find(id))
    }

    @Test
    fun `editing something that has already been deleted closes the form`() = runTest {
        val state = editing(404).state.filter { it.loaded }.first()
        assertTrue("nothing to edit, so nothing to show", state.finished)
    }

    @Test
    fun `resetting blanks the form`() = runTest {
        val form = adding()
        form.amountChanged("45,50")
        form.noteChanged("lunch")
        form.saveAndSettle()
        form.reset()

        assertEquals("", form.state.value.amountText)
        assertEquals("", form.state.value.note)
        assertFalse(form.state.value.finished)
    }

    @Test
    fun `a double tap on Save inserts one expense, not two`() = runTest {
        // A dispatcher that queues rather than running eagerly, so the second
        // press lands while the first save is still in flight - which is the
        // whole of the bug.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val form = adding()
        form.amountChanged("45,50")

        form.save()
        form.save()

        assertFalse("the button is spent while saving", form.state.value.canSave)

        advanceUntilIdle()
        form.state.filter { it.finished }.first()

        val all = expenses.observeDays().first().flatMap { it.expenses }
        assertEquals(1, all.size)
        assertEquals(45.50, all.single().amount, 1e-9)
    }

    /**
     * Saves, and waits for the save to have happened.
     *
     * save() launches into the view model's scope and returns at its first
     * suspension, so reading the database straight afterwards is a race
     * against Room's own executor — one that a developer machine wins and a
     * CI runner does not. The form sets finished once the write is done.
     */
    private suspend fun ExpenseFormViewModel.saveAndSettle() {
        save()
        state.filter { it.finished }.first()
    }

    private fun adding() = ExpenseFormViewModel(expenses, expenseId = null, zone = zone)

    private fun editing(id: Long) = ExpenseFormViewModel(expenses, expenseId = id, zone = zone)
}
