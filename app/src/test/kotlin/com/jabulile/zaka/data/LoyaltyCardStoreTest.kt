package com.jabulile.zaka.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.LoyaltyCard
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoyaltyCardStoreTest {
    private lateinit var database: ZakaDatabase
    private lateinit var cards: LoyaltyCardRepository

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZakaDatabase::class.java,
        ).allowMainThreadQueries().build()
        cards = LoyaltyCardRepository(database.cards())
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `a card round trips through the store`() = runTest {
        val id = cards.save(
            LoyaltyCard(
                name = "Clicks",
                number = "6001087001618",
                symbology = BarcodeSymbology.EAN13,
                tintHex = "0F8E6F",
                addedAt = Instant.parse("2026-11-01T06:00:00Z"),
            ),
        )

        val saved = cards.find(id)!!
        assertEquals("Clicks", saved.name)
        assertEquals("6001087001618", saved.number)
        assertEquals(BarcodeSymbology.EAN13, saved.symbology)
        assertEquals("0F8E6F", saved.tintHex)
        assertEquals("1618", saved.lastFour)
    }

    @Test
    fun `the symbology is stored as its raw value`() = runTest {
        val id = cards.save(
            LoyaltyCard(
                name = "Pick n Pay",
                number = "6001087001618",
                symbology = BarcodeSymbology.QR,
                addedAt = Instant.EPOCH,
            ),
        )
        assertEquals("qr", database.cards().find(id)!!.symbologyRaw)
        assertEquals(BarcodeSymbology.QR, cards.find(id)!!.symbology)
    }

    @Test
    fun `cards come back newest first`() = runTest {
        cards.save(card("Older", Instant.parse("2026-11-01T06:00:00Z")))
        cards.save(card("Newer", Instant.parse("2026-11-20T06:00:00Z")))

        assertEquals(listOf("Newer", "Older"), cards.observeAll().first().map { it.name })
    }

    @Test
    fun `editing a card keeps one row`() = runTest {
        val id = cards.save(card("Dis-Chem", Instant.EPOCH))
        cards.save(cards.find(id)!!.copy(name = "Dis-Chem Baby"))

        assertEquals(listOf("Dis-Chem Baby"), cards.observeAll().first().map { it.name })
    }

    @Test
    fun `deleting removes the card`() = runTest {
        val id = cards.save(card("Woolworths", Instant.EPOCH))
        cards.delete(id)
        assertNull(cards.find(id))
        assertEquals(0, cards.observeAll().first().size)
    }

    private fun card(name: String, addedAt: Instant) = LoyaltyCard(
        name = name,
        number = "6001087001618",
        symbology = BarcodeSymbology.EAN13,
        addedAt = addedAt,
    )
}
