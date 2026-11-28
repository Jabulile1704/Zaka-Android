package com.jabulile.zaka

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app, on a real Android runtime.
 *
 * Deliberately thin. The unit suites already cover what the app computes;
 * what no unit test can tell you is whether the thing starts — whether the
 * manifest is right, the application class is found, Room opens its file and
 * the first frame draws. That is what breaks on a device and passes in CI.
 */
@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test
    fun theAppStartsAndStays() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(false, activity.isFinishing)
            }
        }
    }

    @Test
    fun theApplicationExposesItsStore() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as ZakaApplication

        // Touching these opens the database for real, which is the half of
        // Room that a Robolectric test does not exercise.
        application.expenses
        application.cards
        application.preferences
    }
}
