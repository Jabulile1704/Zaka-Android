package com.jabulile.zaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jabulile.zaka.ui.ZakaApp
import com.jabulile.zaka.ui.theme.ZakaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val zaka = application as ZakaApplication

        setContent {
            ZakaTheme {
                ZakaApp(expenses = zaka.expenses, cards = zaka.cards)
            }
        }
    }
}
