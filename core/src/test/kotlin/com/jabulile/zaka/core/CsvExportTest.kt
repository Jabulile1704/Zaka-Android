package com.jabulile.zaka.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {
    private val zone: ZoneId = ZoneId.of("Africa/Johannesburg")
    private val today: LocalDate = LocalDate.of(2026, 11, 25)

    @Test
    fun `the header and the columns are the iOS ones`() {
        val csv = CsvExport.expenses(listOf(expense(249.90, "Uber home")), zone, today)

        val lines = csv.text.trimEnd('\n').split("\n")
        assertEquals("Date,Category,Note,Amount", lines[0])
        assertEquals(
            "\"2026-11-22 19:45\",\"Transport\",\"Uber home\",249.90",
            lines[1],
        )
    }

    @Test
    fun `rows are oldest first, whatever order they arrive in`() {
        val csv = CsvExport.expenses(
            listOf(
                expense(3.0, "third", "2026-11-23T10:00:00Z"),
                expense(1.0, "first", "2026-11-21T10:00:00Z"),
                expense(2.0, "second", "2026-11-22T10:00:00Z"),
            ),
            zone,
            today,
        )
        val notes = csv.text.lines().drop(1).filter { it.isNotEmpty() }.map { it.split(",")[2] }
        assertEquals(listOf("\"first\"", "\"second\"", "\"third\""), notes)
    }

    @Test
    fun `amounts are plain numbers a spreadsheet will add up`() {
        val csv = CsvExport.expenses(
            listOf(expense(1234.5, ""), expense(0.5, ""), expense(1234567.89, "")),
            zone,
            today,
        )
        val amounts = csv.text.lines().drop(1).filter { it.isNotEmpty() }.map { it.split(",")[3] }

        assertEquals(listOf("0.50", "1234.50", "1234567.89"), amounts.sorted())
        assertFalse("no currency symbol", csv.text.contains("R"))
        assertFalse("no thousands separator inside a number", csv.text.contains("1,234.50"))
    }

    @Test
    fun `an awkward note survives intact`() {
        val awkward = "Lunch, \"the usual\"\nwith Thandi"
        val csv = CsvExport.expenses(listOf(expense(45.5, awkward)), zone, today)

        assertTrue(
            "quotes doubled, commas and newlines inside the quoted field",
            csv.text.contains("\"Lunch, \"\"the usual\"\"\nwith Thandi\""),
        )
        // Four fields, so three commas outside the quoted parts.
        assertEquals(1, csv.text.trimEnd('\n').split("\n").size - 2)
    }

    @Test
    fun `an empty note is an empty field, not a word`() {
        val csv = CsvExport.expenses(listOf(expense(10.0, "")), zone, today)
        assertTrue(csv.text.contains(",\"\","))
        assertFalse(csv.text.contains("null"))
    }

    @Test
    fun `the category is the label a person reads`() {
        val csv = CsvExport.expenses(
            listOf(
                Expense(
                    amount = 1.0,
                    category = ExpenseCategory.OTHERS,
                    date = Instant.parse("2026-11-22T17:45:00Z"),
                ),
            ),
            zone,
            today,
        )
        assertTrue("the label, not the stored key", csv.text.contains("\"Others\""))
        assertFalse(csv.text.contains("\"other\""))
    }

    @Test
    fun `stamps are in the device's zone`() {
        // 22:30 UTC is already the next day in Johannesburg.
        val csv = CsvExport.expenses(
            listOf(expense(1.0, "", "2026-11-22T22:30:00Z")),
            zone,
            today,
        )
        assertTrue(csv.text.contains("\"2026-11-23 00:30\""))
    }

    @Test
    fun `an empty export is a header and nothing else`() {
        val csv = CsvExport.expenses(emptyList(), zone, today)
        assertEquals("Date,Category,Note,Amount\n", csv.text)
    }

    @Test
    fun `the file is named for the day it was exported`() {
        assertEquals(
            "Zaka-expenses-2026-11-25.csv",
            CsvExport.expenses(emptyList(), zone, today).name,
        )
        assertEquals(
            "Zaka-cards-2026-11-25.csv",
            CsvExport.cards(emptyList(), zone, today).name,
        )
    }

    @Test
    fun `cards export their shop, number, type and when they were added`() {
        val csv = CsvExport.cards(
            listOf(
                LoyaltyCard(
                    name = "Dis-Chem",
                    number = "6001087001618",
                    symbology = BarcodeSymbology.EAN13,
                    addedAt = Instant.parse("2026-11-01T06:00:00Z"),
                ),
            ),
            zone,
            today,
        )

        val lines = csv.text.trimEnd('\n').split("\n")
        assertEquals("Shop,Number,Type,Added", lines[0])
        assertEquals(
            "\"Dis-Chem\",\"6001087001618\",\"EAN-13\",\"2026-11-01 08:00\"",
            lines[1],
        )
    }

    @Test
    fun `a card number keeps its leading zeros`() {
        val csv = CsvExport.cards(
            listOf(
                LoyaltyCard(
                    name = "Shop",
                    number = "0001234",
                    symbology = BarcodeSymbology.CODE128,
                    addedAt = Instant.EPOCH,
                ),
            ),
            zone,
            today,
        )
        assertTrue("quoted, so a spreadsheet keeps it as text", csv.text.contains("\"0001234\""))
    }

    @Test
    fun `every document ends with a newline`() {
        assertTrue(CsvExport.expenses(emptyList(), zone, today).text.endsWith("\n"))
        assertTrue(
            CsvExport.expenses(listOf(expense(1.0, "x")), zone, today).text.endsWith("\n"),
        )
    }

    private fun expense(
        amount: Double,
        note: String,
        at: String = "2026-11-22T17:45:00Z",
    ) = Expense(
        amount = amount,
        category = ExpenseCategory.TRANSPORT,
        date = Instant.parse(at),
        note = note,
    )
}
