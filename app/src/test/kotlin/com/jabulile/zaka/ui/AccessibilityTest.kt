package com.jabulile.zaka.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.DayGroup
import com.jabulile.zaka.core.Expense
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.core.SpendBreakdown
import com.jabulile.zaka.ui.cards.CardsScreen
import com.jabulile.zaka.ui.cards.CardsUiState
import com.jabulile.zaka.ui.expenses.ExpensesScreen
import com.jabulile.zaka.ui.expenses.ExpensesUiState
import com.jabulile.zaka.ui.insights.InsightsScreen
import com.jabulile.zaka.ui.insights.InsightsUiState
import com.jabulile.zaka.ui.theme.ZakaTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What a screen reader is handed, asserted without a device.
 *
 * Android lint's accessibility checks are written for Views and have almost
 * nothing to say about Compose, so these are the tests that actually hold the
 * semantics in place.
 */
@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val zone: ZoneId = ZoneId.of("Africa/Johannesburg")
    private val today: LocalDate = LocalDate.of(2026, 11, 25)

    @Test
    fun `a day heading is one announcement, and a heading`() {
        compose.setContent {
            ZakaTheme(dynamicColor = false) {
                ExpensesScreen(state = expensesState(), onEdit = {}, onDelete = {})
            }
        }

        compose.onNodeWithContentDescription("Today, R150.00")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `a row can be deleted without the swipe gesture`() {
        val deleted = mutableListOf<Long>()
        compose.setContent {
            ZakaTheme(dynamicColor = false) {
                ExpensesScreen(
                    state = expensesState(),
                    onEdit = {},
                    onDelete = { deleted += it },
                )
            }
        }

        val actions = compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        assertEquals("Delete this expense", actions.single().label)

        actions.single().action()

        assertEquals("the swipe and the action do the same thing", listOf(7L), deleted)
    }

    @Test
    fun `a breakdown row announces its share and its amount together`() {
        compose.setContent {
            ZakaTheme(dynamicColor = false) {
                InsightsScreen(
                    state = insightsState(),
                    onPrevious = {},
                    onNext = {},
                    onStyle = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Groceries, 75%, R150.00").assertExists()
        compose.onNodeWithContentDescription("Airtime, 25%, R50.00").assertExists()
    }

    @Test
    fun `the month arrows are not unlabelled icons`() {
        compose.setContent {
            ZakaTheme(dynamicColor = false) {
                InsightsScreen(
                    state = insightsState(),
                    onPrevious = {},
                    onNext = {},
                    onStyle = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Previous month").assertExists()
        compose.onNodeWithContentDescription("Next month").assertExists()
    }

    @Test
    fun `a card can be edited and deleted without a long press`() {
        val edited = mutableListOf<Long>()
        val deleted = mutableListOf<Long>()

        compose.setContent {
            ZakaTheme(dynamicColor = false) {
                CardsScreen(
                    state = CardsUiState(
                        cards = listOf(
                            LoyaltyCard(
                                id = 3,
                                name = "Clicks",
                                number = "6001087001618",
                                symbology = BarcodeSymbology.EAN13,
                                addedAt = java.time.Instant.EPOCH,
                            ),
                        ),
                        loaded = true,
                    ),
                    onShow = {},
                    onEdit = { edited += it },
                    onDelete = { deleted += it },
                    onAdd = {},
                )
            }
        }

        val actions = compose
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]

        assertEquals(
            listOf("Edit this card", "Delete this card"),
            actions.map { it.label },
        )

        actions[0].action()
        actions[1].action()

        assertEquals(listOf(3L), edited)
        assertEquals(listOf(3L), deleted)
    }

    private fun expensesState(): ExpensesUiState {
        val expense = Expense(
            id = 7,
            amount = 150.0,
            category = ExpenseCategory.TRANSPORT,
            date = today.atTime(12, 0).atZone(zone).toInstant(),
            note = "Uber home",
        )
        return ExpensesUiState(
            days = DayGroup.group(listOf(expense), zone),
            monthTotal = 150.0,
            today = today,
            loaded = true,
        )
    }

    private fun insightsState(): InsightsUiState {
        val totals = mapOf(
            ExpenseCategory.GROCERIES to 150.0,
            ExpenseCategory.AIRTIME to 50.0,
        )
        return InsightsUiState(
            month = YearMonth.of(2026, 11),
            total = 200.0,
            perDay = 8.0,
            slices = SpendBreakdown.slices(totals),
            canGoForward = true,
            loaded = true,
        )
    }
}
