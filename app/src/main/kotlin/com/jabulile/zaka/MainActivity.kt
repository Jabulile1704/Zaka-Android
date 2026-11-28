package com.jabulile.zaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jabulile.zaka.ui.ZakaApp
import com.jabulile.zaka.ui.theme.ZakaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, as the platform requires. The splash leaves
        // as soon as the first frame is drawn: the store is local and opens
        // instantly, so holding it any longer would be theatre.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val zaka = application as ZakaApplication

        setContent {
            ZakaTheme {
                ZakaApp(
                    expenses = zaka.expenses,
                    cards = zaka.cards,
                    preferences = zaka.preferences,
                )
            }
        }
    }
}
