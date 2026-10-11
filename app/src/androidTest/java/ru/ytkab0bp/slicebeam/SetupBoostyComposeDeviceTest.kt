package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.AbstractComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Opens the standalone supporter route and verifies its real Compose view attaches on Android. */
@RunWith(AndroidJUnit4::class)
class SetupBoostyComposeDeviceTest {
    @Test
    fun onboardingPagerMovesBetweenComposePagesAndRestoresAfterRecreation() {
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
            .putExtra(SetupActivity.EXTRA_ADD_PRINTER, true)
        val scenario = ActivityScenario.launch<SetupActivity>(intent)
        try {
            val advancePage = SetupActivity::class.java.getDeclaredMethod("scrollToNext").apply {
                isAccessible = true
            }
            scenario.onActivity { activity -> advancePage.invoke(activity) }
            waitForSetupPage(scenario, 1)
            assertSetupPageContainsComposeContent(scenario)

            scenario.recreate()
            waitForSetupPage(scenario, 1)
            assertSetupPageContainsComposeContent(scenario)

            scenario.onActivity { activity -> advancePage.invoke(activity) }
            waitForSetupPage(scenario, 2)
            assertSetupPageContainsComposeContent(scenario)

            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            waitForSetupPage(scenario, 1)
            assertSetupPageContainsComposeContent(scenario)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun supporterPageOpensAsComposeWithoutCrashing() {
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
            .putExtra(SetupActivity.EXTRA_BOOSTY_ONLY, true)
        val scenario = ActivityScenario.launch<SetupActivity>(intent)
        try {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(
                    "Supporter route should render its Compose screen",
                    containsAttachedComposeView(activity.window.decorView),
                )
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun aboutPageRendersInsideComposeRootAndSurvivesRecreation() {
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
            .putExtra(SetupActivity.EXTRA_ABOUT, true)
        val scenario = ActivityScenario.launch<SetupActivity>(intent)
        try {
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(
                    "Setup artwork and About content should both render in Compose",
                    attachedComposeViewCount(activity.window.decorView) == 1,
                )
                val themeHost = findThemeAwareComposeView(activity.window.decorView)
                assertTrue(
                    "The theme-aware host should directly inherit AbstractComposeView",
                    ThemeAwareComposeView::class.java.superclass == AbstractComposeView::class.java,
                )
                val previousRevision = themeHost.themeRevision
                themeHost.onApplyTheme()
                assertTrue("Theme invalidation should advance the Compose revision", themeHost.themeRevision == previousRevision + 1)
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(
                    "Compose root and About content should reattach after Activity recreation",
                    attachedComposeViewCount(activity.window.decorView) == 1,
                )
            }
        } finally {
            scenario.close()
        }
    }

    private fun containsAttachedComposeView(view: View): Boolean {
        if (view is AbstractComposeView) return view.isAttachedToWindow
        if (view is ViewGroup) {
            return (0 until view.childCount).any { index -> containsAttachedComposeView(view.getChildAt(index)) }
        }
        return false
    }

    private fun attachedComposeViewCount(view: View): Int {
        val composeViewCount = if (view is AbstractComposeView && view.isAttachedToWindow) 1 else 0
        if (view is ViewGroup) {
            return composeViewCount + (0 until view.childCount).sumOf { index ->
                attachedComposeViewCount(view.getChildAt(index))
            }
        }
        return composeViewCount
    }

    private fun findThemeAwareComposeView(view: View): ThemeAwareComposeView {
        if (view is ThemeAwareComposeView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                runCatching { findThemeAwareComposeView(view.getChildAt(index)) }
                    .getOrNull()
                    ?.let { return it }
            }
        }
        throw AssertionError("The setup screen has no theme-aware Compose root")
    }

    private fun waitForSetupPage(scenario: ActivityScenario<SetupActivity>, expectedPage: Int) {
        val pageGetter = SetupActivity::class.java.getDeclaredMethod("getCurrentSetupPage").apply {
            isAccessible = true
        }
        val page = AtomicInteger(-1)
        repeat(80) {
            scenario.onActivity { activity -> page.set(pageGetter.invoke(activity) as Int) }
            if (page.get() == expectedPage) return
            Thread.sleep(100)
        }
        assertTrue("Setup pager should settle on page $expectedPage; was ${page.get()}", page.get() == expectedPage)
    }

    private fun assertSetupPageContainsComposeContent(scenario: ActivityScenario<SetupActivity>) {
        scenario.onActivity { activity ->
            assertTrue(
                "Setup page content should render directly in the single Compose root",
                attachedComposeViewCount(activity.window.decorView) == 1,
            )
        }
    }
}
