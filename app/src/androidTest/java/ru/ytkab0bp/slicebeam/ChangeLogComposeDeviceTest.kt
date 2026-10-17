package ru.ytkab0bp.slicebeam

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.ChangeLogDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the Compose-owned release notes route on the Honor device. */
@RunWith(AndroidJUnit4::class)
class ChangeLogComposeDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun releaseNotesAndSupporterSheetRenderAndDismissWithAndroidBack() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        lateinit var dialog: ChangeLogDialog
        compose.runOnUiThread { dialog = ChangeLogDialog(compose.activity) }
        val dismissed = mutableStateOf(false)
        compose.setContent {
            BeamComposeTheme {
                if (!dismissed.value) dialog.Content { dismissed.value = true }
            }
        }

        compose.onNodeWithText(compose.activity.getString(R.string.Changelog)).assertIsDisplayed()
        compose.onNodeWithText("OrkanSlicer 0.8.296", substring = true).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(dismissed.value) }
    }
}
