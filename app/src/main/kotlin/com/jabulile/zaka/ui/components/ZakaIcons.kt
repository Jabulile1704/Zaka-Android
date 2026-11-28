package com.jabulile.zaka.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.TakeoutDining
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.jabulile.zaka.core.ExpenseCategory

/**
 * Every icon the app draws, in one file.
 *
 * `ExpenseCategory.iconName` in `:core` records which Material Symbol each
 * category wants; this is the one place that turns that intent into a drawable,
 * so a change of icon set is a change to this file and nothing else.
 */
object ZakaIcons {
    val Expenses: ImageVector = Icons.Filled.Receipt
    val Add: ImageVector = Icons.Filled.Add
    val Insights: ImageVector = Icons.Filled.PieChart
    val Cards: ImageVector = Icons.Filled.CardMembership

    val Delete: ImageVector = Icons.Filled.Delete
    val Export: ImageVector = Icons.Filled.FileDownload
    val Scan: ImageVector = Icons.Filled.PhotoCamera
    val Close: ImageVector = Icons.Filled.Close
    val Save: ImageVector = Icons.Filled.Check
    val Previous: ImageVector = Icons.Filled.ChevronLeft
    val Next: ImageVector = Icons.Filled.ChevronRight

    fun of(category: ExpenseCategory): ImageVector = when (category) {
        ExpenseCategory.TAKEAWAYS -> Icons.Filled.TakeoutDining
        ExpenseCategory.GROCERIES -> Icons.Filled.ShoppingBasket
        ExpenseCategory.TRANSPORT -> Icons.Filled.DirectionsCar
        ExpenseCategory.RENT -> Icons.Filled.Home
        ExpenseCategory.UTILITIES -> Icons.Filled.Bolt
        ExpenseCategory.HEALTH -> Icons.Filled.MedicalServices
        ExpenseCategory.WIFI -> Icons.Filled.Wifi
        ExpenseCategory.SUBSCRIPTIONS -> Icons.Filled.Autorenew
        ExpenseCategory.SHOPPING -> Icons.Filled.ShoppingBag
        ExpenseCategory.ACCOUNTS -> Icons.Filled.CreditCard
        ExpenseCategory.CASH -> Icons.Filled.Payments
        ExpenseCategory.TRAVELLING -> Icons.Filled.Flight
        ExpenseCategory.DATES -> Icons.Filled.Favorite
        ExpenseCategory.AIRTIME -> Icons.Filled.CellTower
        ExpenseCategory.HAIRCUT -> Icons.Filled.ContentCut
        ExpenseCategory.OTHERS -> Icons.Filled.GridView
    }
}
