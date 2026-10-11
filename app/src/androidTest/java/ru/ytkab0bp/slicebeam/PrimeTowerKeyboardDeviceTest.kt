package ru.ytkab0bp.slicebeam

import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.fragment.PrimeTowerPositionEditor
import ru.ytkab0bp.slicebeam.fragment.PrintConfigTowerPositions
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Real dialog window, IME visibility and numeric typing on the attached phone. */
@RunWith(AndroidJUnit4::class)
class PrimeTowerKeyboardDeviceTest {
    @Test fun tappedTowerEditorOpensKeyboardAndAcceptsBothCoordinates() = exerciseEditor(1, false)

    @Test fun allPlatesEditorInWrappedComposeViewAcceptsBothCoordinates() = exerciseEditor(2, true)

    private fun exerciseEditor(plateCount: Int, wrapComposeView: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        val x = mutableStateListOf<String>().apply { repeat(plateCount) { add("") } }
        val y = mutableStateListOf<String>().apply { repeat(plateCount) { add("") } }
        val scenario = ActivityScenario.launch(SafeStartActivity::class.java)
        var dialog: AlertDialog? = null
        try {
            scenario.onActivity { activity ->
                val content = ComposeView(activity).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        BeamComposeTheme {
                            PrimeTowerPositionEditor(
                                positions = PrintConfigTowerPositions(plateCount, x.toList(), y.toList()),
                                xValues = x.toList(), yValues = y.toList(), error = false,
                                onXChanged = { index, value -> x[index] = value },
                                onYChanged = { index, value -> y[index] = value },
                            )
                        }
                    }
                }
                val customView = if (wrapComposeView) FrameLayout(activity).apply { addView(content) } else content
                dialog = BeamAlertDialogBuilder(activity)
                    .setTitle(R.string.PrimeTowerPosition)
                    .setView(customView)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().also { created ->
                        created.setOnShowListener {
                            created.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        }
                        created.show()
                    }
            }
            val deadline = SystemClock.uptimeMillis() + 10_000
            var keyboardVisible = false
            while (!keyboardVisible && SystemClock.uptimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    keyboardVisible = dialog?.window?.decorView?.let { view ->
                        ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime())
                    } == true
                }
                if (!keyboardVisible) SystemClock.sleep(100)
            }
            assertTrue("Opening the position editor must show the system keyboard", keyboardVisible)
            scenario.onActivity {
                assertEquals(0, requireNotNull(dialog?.window).attributes.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }
            instrumentation.sendStringSync("27.5")
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertEquals("X must receive the typed decimal coordinate", "27.5", x[0]) }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            instrumentation.sendStringSync("42.25")
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals("Y must receive the second decimal coordinate", "42.25", y[0])
                assertEquals("Editing Y must retain X", "27.5", x[0])
                if (plateCount > 1) {
                    assertEquals("Other plates must stay untouched", "", x[1])
                    assertEquals("Other plates must stay untouched", "", y[1])
                }
            }
        } finally {
            scenario.onActivity { dialog?.dismiss() }
            scenario.close()
        }
    }
}
