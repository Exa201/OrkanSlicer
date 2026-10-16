package ru.ytkab0bp.slicebeam

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceLegacyModeHost
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Verifies renderer-owned Views stay attached through the shared Compose mode boundary. */
@RunWith(AndroidJUnit4::class)
class WorkspaceLegacyModeHostDeviceTest {
    @Test
    fun legacyModeViewIsMountedAndReleasedByComposeBoundary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val prefs = requireNotNull(Prefs.getPrefs())
        val previousCrash = runCatching { prefs.getString("crash", null) }.getOrNull()
        val previousPrompt = prefs.getBoolean("converter_storage_first_prompt", false)
        val previousLastCommit = prefs.getString("last_commit", null)
        val previousConfig = SliceBeam.CONFIG
        prefs.edit().remove("crash").putBoolean("converter_storage_first_prompt", true).commit()
        Prefs.setLastCommit()
        val config = context.assets.open("orca_profiles/Anycubic.ini").use { Slic3rConfigWrapper(it) }
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val mounted = AtomicBoolean(false)
        val released = AtomicBoolean(false)
        val completed = CountDownLatch(1)
        try {
            scenario.onActivity { activity ->
                val modeView = TextView(activity).apply { text = "GL mode" }
                val root = FrameLayout(activity)
                val composeHost = ComposeView(activity).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent { BeamComposeTheme { WorkspaceLegacyModeHost(modeView) } }
                }
                activity.addContentView(root, ViewGroup.LayoutParams(-1, -1))
                root.addView(composeHost, FrameLayout.LayoutParams(-1, -1))
                root.post {
                    mounted.set(modeView.parent is ViewGroup)
                    root.removeView(composeHost)
                    root.post {
                        released.set(modeView.parent == null)
                        completed.countDown()
                    }
                }
            }
            assertTrue("Compose should mount and release the AndroidView", completed.await(5, TimeUnit.SECONDS))
            assertTrue("the existing mode View is attached beneath Compose", mounted.get())
            assertTrue("disposing the Compose boundary releases the mode view", released.get())
        } finally {
            scenario.close()
            SliceBeam.CONFIG = previousConfig
            prefs.edit()
                .also { editor ->
                    if (previousCrash == null) editor.remove("crash") else editor.putString("crash", previousCrash)
                }
                .putBoolean("converter_storage_first_prompt", previousPrompt)
                .also { editor ->
                    if (previousLastCommit == null) editor.remove("last_commit") else editor.putString("last_commit", previousLastCommit)
                }
                .commit()
        }
    }
}
