package com.jabulile.zaka.ui.cards

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.data.LoyaltyCardRepository
import com.jabulile.zaka.data.ZakaDatabase
import java.time.Instant
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CardsViewModelTest {
    private lateinit var database: ZakaDatabase
    private lateinit var cards: LoyaltyCardRepository
    private lateinit var viewModel: CardsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        cards = LoyaltyCardRepository(database.cards())
        viewModel = CardsViewModel(cards)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `cards reach the screen newest first`() = runTest {
        cards.save(card("Older", Instant.parse("2026-11-01T06:00:00Z")))
        cards.save(card("Newer", Instant.parse("2026-11-20T06:00:00Z")))

        val state = viewModel.state.filter { it.loaded }.first()
        assertEquals(listOf("Newer", "Older"), state.cards.map { it.name })
    }

    @Test
    fun `an empty wallet still produces a state`() = runTest {
        val state = viewModel.state.filter { it.loaded }.first()
        assertTrue(state.cards.isEmpty())
    }

    @Test
    fun `deleting a card drops it from the screen`() = runTest {
        val id = cards.save(card("Clicks", Instant.EPOCH))
        assertEquals(1, viewModel.state.filter { it.loaded }.first().cards.size)

        viewModel.delete(id)

        assertTrue(viewModel.state.filter { it.loaded && it.cards.isEmpty() }.first().cards.isEmpty())
    }

    @Test
    fun `exporting offers the cards the iOS app has never exported`() = runTest {
        cards.save(card("Clicks", Instant.parse("2026-11-01T06:00:00Z")))
        viewModel.state.filter { it.loaded }.first()

        viewModel.export()

        val csv = viewModel.state.filter { it.export != null }.first().export!!
        assertTrue(csv.text.startsWith("Shop,Number,Type,Added"))
        assertTrue(csv.text.contains("\"Clicks\""))
        assertTrue(csv.name.startsWith("Zaka-cards-"))

        viewModel.exportShared()
        assertTrue(viewModel.state.filter { it.export == null }.first().export == null)
    }

    private fun card(name: String, addedAt: Instant) = LoyaltyCard(
        name = name,
        number = "6001087001618",
        symbology = BarcodeSymbology.EAN13,
        addedAt = addedAt,
    )
}
