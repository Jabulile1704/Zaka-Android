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
class CardFormViewModelTest {
    private val ean = "6001087001618"

    private lateinit var database: ZakaDatabase
    private lateinit var cards: LoyaltyCardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        cards = LoyaltyCardRepository(database.cards())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the type follows the number until the user picks one`() = runTest {
        val form = adding()

        form.numberChanged("12345")
        assertEquals(BarcodeSymbology.CODE128, form.state.value.symbology)

        form.numberChanged(ean)
        assertEquals("thirteen valid digits read as EAN-13", BarcodeSymbology.EAN13, form.state.value.symbology)

        form.symbologyChosen(BarcodeSymbology.CODE128)
        form.numberChanged("6001087001618")
        assertEquals(
            "a chosen type must not move under the typing",
            BarcodeSymbology.CODE128,
            form.state.value.symbology,
        )
    }

    @Test
    fun `a card that could not be shown cannot be saved`() = runTest {
        val form = adding()
        assertFalse("nothing typed", form.state.value.canSave)

        form.nameChanged("Clicks")
        form.symbologyChosen(BarcodeSymbology.EAN13)
        form.numberChanged("6001087001610")
        assertFalse("a bad check digit cannot be drawn as EAN-13", form.state.value.canSave)

        form.numberChanged(ean)
        assertTrue(form.state.value.canSave)

        form.nameChanged("   ")
        assertFalse("a card with no shop name is not useful in a list", form.state.value.canSave)
    }

    @Test
    fun `saving writes what was entered`() = runTest {
        val form = adding()
        form.nameChanged("  Dis-Chem  ")
        form.numberChanged(ean)
        form.tintChosen("B3246B")
        form.save()

        val saved = cards.observeAll().first().single()
        assertEquals("Dis-Chem", saved.name)
        assertEquals(ean, saved.number)
        assertEquals(BarcodeSymbology.EAN13, saved.symbology)
        assertEquals("B3246B", saved.tintHex)
        assertEquals("1618", saved.lastFour)
    }

    @Test
    fun `a double tap saves one card, not two`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val form = adding()
        form.nameChanged("Clicks")
        form.numberChanged(ean)

        form.save()
        form.save()
        assertFalse(form.state.value.canSave)

        advanceUntilIdle()
        assertEquals(1, cards.observeAll().first().size)
    }

    @Test
    fun `editing keeps the card's place in the list`() = runTest {
        val added = Instant.parse("2026-11-01T06:00:00Z")
        val id = cards.save(
            LoyaltyCard(
                name = "Pick n Pay",
                number = ean,
                symbology = BarcodeSymbology.EAN13,
                addedAt = added,
            ),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        form.nameChanged("Pick n Pay Smart Shopper")
        form.save()

        val saved = cards.find(id)!!
        assertEquals("Pick n Pay Smart Shopper", saved.name)
        assertEquals("the list is ordered by this", added, saved.addedAt)
        assertEquals(1, cards.observeAll().first().size)
    }

    @Test
    fun `a saved type is treated as deliberate`() = runTest {
        val id = cards.save(
            LoyaltyCard(
                name = "Woolworths",
                number = ean,
                symbology = BarcodeSymbology.CODE128,
                addedAt = Instant.EPOCH,
            ),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        // Re-typing the same digits must not promote it to EAN-13.
        form.numberChanged(ean)

        assertEquals(BarcodeSymbology.CODE128, form.state.value.symbology)
    }

    @Test
    fun `deleting from the form removes the card`() = runTest {
        val id = cards.save(
            LoyaltyCard(
                name = "Clicks",
                number = ean,
                symbology = BarcodeSymbology.EAN13,
                addedAt = Instant.EPOCH,
            ),
        )

        val form = editing(id)
        form.state.filter { it.loaded }.first()
        form.delete()

        // Same reason as the expense form: delete() returns at the first
        // suspension, so the row is only certainly gone once the form says
        // it has finished.
        form.state.filter { it.finished }.first()

        assertNull(cards.find(id))
    }

    @Test
    fun `editing a card that has gone closes the form`() = runTest {
        assertTrue(editing(404).state.filter { it.loaded }.first().finished)
    }

    private fun adding() = CardFormViewModel(cards, cardId = null)

    private fun editing(id: Long) = CardFormViewModel(cards, cardId = id)
}
