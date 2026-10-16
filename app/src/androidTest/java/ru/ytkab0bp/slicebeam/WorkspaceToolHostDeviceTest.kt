package ru.ytkab0bp.slicebeam

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.UnfoldMenu

/** Exercises the real UnfoldMenu compatibility route through the new Compose presentation host. */
@RunWith(AndroidJUnit4::class)
class WorkspaceToolHostDeviceTest {
    @Test
    fun routeAttachesAsComposeHostAndRunsLifecycleOnDismiss() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val menu = ProbeMenu()
        menu.setOnDismiss { menu.dismissed++ }
        var root: FrameLayout? = null
        try {
            scenario.onActivity { activity ->
                root = FrameLayout(activity)
                val anchor = View(activity)
                root!!.addView(anchor, FrameLayout.LayoutParams(1, 1))
                activity.addContentView(
                    root,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                root!!.post { menu.show(anchor, root!!) }
            }
            waitUntil(instrumentation) { menu.isAttached() && menu.created == 1 }
            scenario.onActivity { activity ->
                val parent = requireNotNull(root)
                assertEquals(2, parent.childCount)
                assertTrue(parent.getChildAt(1) is ComposeView)
                menu.dismiss()
            }
            waitUntil(instrumentation) { !menu.isAttached() && menu.destroyed == 1 }
            assertEquals(1, menu.dismissed)
        } finally {
            scenario.close()
        }
    }

    private fun waitUntil(instrumentation: android.app.Instrumentation, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(30L)
        }
        throw AssertionError("Tool host did not reach the expected lifecycle state")
    }

    private class ProbeMenu : UnfoldMenu() {
        var created = 0
        var destroyed = 0
        var dismissed = 0

        override fun onCreateView(ctx: android.content.Context, portrait: Boolean): View =
            TextView(ctx).apply { text = "Compose hosted route" }

        override fun onCreate() {
            created++
            super.onCreate()
        }

        override fun onDestroy() {
            destroyed++
            super.onDestroy()
        }
    }
}

