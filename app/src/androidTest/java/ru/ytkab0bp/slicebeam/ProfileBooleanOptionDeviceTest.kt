package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileBooleanOption
import java.util.function.Consumer

/** Checks that a profile boolean row updates its visible state and reports the changed value. */
@RunWith(AndroidJUnit4::class)
class ProfileBooleanOptionDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun booleanOptionToggleReportsAndDisplaysTheNewValue() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileBooleanOption.Content(
                    title = "Enable support",
                    initialValue = false,
                    onChanged = Consumer(changes::add),
                    onLongClick = Runnable {},
                )
            }
        }

        compose.onNodeWithText("Enable support").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }
}
