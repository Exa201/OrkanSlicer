package ru.ytkab0bp.slicebeam

import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.ChangeLogBottomSheet
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Opens the release-note sheet in-app and checks its actual Compose surface on Android 16. */
@RunWith(AndroidJUnit4::class)
class ChangeLogComposeDeviceTest {
    @Test
    fun releaseNotesAndSupporterSheetRenderInsideCompose() {
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
        var sheet: ChangeLogBottomSheet? = null
        try {
            scenario.onActivity { activity ->
                val createdSheet = ChangeLogBottomSheet(activity)
                sheet = createdSheet
                createdSheet.show()
            }
            instrumentation.waitForIdleSync()
            assertTrue("Release notes should render in a Compose bottom sheet", containsComposeView(requireNotNull(sheet)))
        } finally {
            scenario.onActivity { sheet?.dismiss() }
            scenario.close()
        }
    }

    private fun containsComposeView(dialog: BottomSheetDialog): Boolean {
        val decor = dialog.window?.decorView ?: return false
        return hasAttachedComposeView(decor)
    }

    private fun hasAttachedComposeView(view: View): Boolean {
        if (view is ComposeView) return view.isAttachedToWindow
        if (view is ViewGroup) {
            return (0 until view.childCount).any { index -> hasAttachedComposeView(view.getChildAt(index)) }
        }
        return false
    }
}
