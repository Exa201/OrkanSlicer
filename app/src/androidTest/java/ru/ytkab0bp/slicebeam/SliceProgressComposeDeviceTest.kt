package ru.ytkab0bp.slicebeam

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.SliceProgressDialog
import ru.ytkab0bp.slicebeam.events.SlicingProgressEvent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class SliceProgressComposeDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun slicingProgressStreamsThroughWorkspaceDialogAndCompletes() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        var completions = 0
        lateinit var dialog: SliceProgressDialog
        compose.runOnUiThread { dialog = SliceProgressDialog(compose.activity) { completions++ } }
        compose.setContent { BeamComposeTheme { dialog.Content() } }

        compose.onNodeWithText(compose.activity.getString(R.string.SliceInProgress)).assertIsDisplayed()
        SliceBeam.EVENT_BUS.fireEvent(SlicingProgressEvent(47, "perimeter"))
        compose.waitForIdle()
        compose.onAllNodesWithText(compose.activity.getString(R.string.SliceStepPerimeters))
            .onFirst().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.SliceProgressPercent, 47)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, completions) }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText(compose.activity.getString(R.string.SliceInProgress)).assertIsDisplayed()

        SliceBeam.EVENT_BUS.fireEvent(SlicingProgressEvent(100, ""))
        SliceBeam.EVENT_BUS.fireEvent(SlicingProgressEvent(100, ""))
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, completions) }
    }
}
