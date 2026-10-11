package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Opens the internal cloud setup route inside the app and verifies its Compose surface renders. */
@RunWith(AndroidJUnit4::class)
class SetupCloudProfileDeviceTest {
    @Test
    fun cloudAccountSetupOpensAsComposeWithoutCrashing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
        Prefs.setLastCommit()

        val intent = Intent(context, SetupActivity::class.java)
            .putExtra(SetupActivity.EXTRA_CLOUD_PROFILE, true)
        val scenario = ActivityScenario.launch<SetupActivity>(intent)
        try {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(
                    "Cloud account setup should render its Compose screen",
                    containsComposeView(activity.window.decorView),
                )
            }
        } finally {
            scenario.close()
        }
    }

    private fun containsComposeView(view: View): Boolean {
        if (view is AbstractComposeView) return view.isAttachedToWindow
        if (view is ViewGroup) {
            return (0 until view.childCount).any { index -> containsComposeView(view.getChildAt(index)) }
        }
        return false
    }
}
