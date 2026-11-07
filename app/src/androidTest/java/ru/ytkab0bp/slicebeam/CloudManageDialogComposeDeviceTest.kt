package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.CloudManageDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.Prefs

@RunWith(AndroidJUnit4::class)
class CloudManageDialogComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cloudManagementDialogRendersAndDismissesWithAndroidBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        lateinit var dialog: CloudManageDialog
        compose.runOnUiThread { dialog = CloudManageDialog(app) }
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                if (!dismissed) dialog.Content { dismissed = true }
            }
        }

        compose.onNodeWithText(app.getString(R.string.SettingsCloudManageButtonManage)).assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
