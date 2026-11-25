package com.jabulile.zaka.ui.expenses

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.data.ExpenseRepository
import com.jabulile.zaka.data.ZakaDatabase
import java.time.Instant
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExpenseRepository(database.expenses(), zone)
        viewModel = ExpensesViewModel(repository, zone)
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

    private fun expense(amount: Double, category: ExpenseCategory) = Expense(
        amount = amount,
        category = category,
        // Now, because the month total only counts the current month.
        date = Instant.now(),
    )
}
