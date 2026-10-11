package ru.ytkab0bp.slicebeam

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Confirms the crash-recovery route itself can render without loading printer profiles. */
@RunWith(AndroidJUnit4::class)
class SafeStartComposeDeviceTest {
    @Test
    fun recoveryScreenRendersAfterStoredCrashWithoutOpeningWorkspace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.putString("crash", "Simulated recovery screen test")?.commit()

        val scenario = ActivityScenario.launch(SafeStartActivity::class.java)
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(
                    "The recovery activity should host its content in Compose",
                    hasAttachedComposeView(activity.window.decorView),
                )
                assertTrue("Recovery screen should stay open until the user chooses an action", !activity.isFinishing)
            }
        } finally {
            scenario.close()
            Prefs.getPrefs()?.edit()?.remove("crash")?.commit()
        }
    }

    private fun hasAttachedComposeView(view: View): Boolean {
        if (view is ComposeView) return view.isAttachedToWindow
        if (view is ViewGroup) {
            return (0 until view.childCount).any { index -> hasAttachedComposeView(view.getChildAt(index)) }
        }
        return false
    }
}
