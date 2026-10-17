package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real workspace pre-send confirmation without issuing network or printer actions. */
@RunWith(AndroidJUnit4::class)
class SlicePrintPreparationComposeDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun preparationCancelDoesNotDispatchAndUploadOnlyUsesInjectedAction() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val printer = requireNotNull(SliceBeam.requireConfig().findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        printer.put("host_type", "octoprint")
        printer.put("print_host", "http://printer.invalid")
        val sends = AtomicInteger()
        val printFlags = AtomicReference<Boolean>()
        val sentHost = AtomicReference<String>()

        fun showPreparation() {
            SlicePrintPreparationDialog.show(
                fragment = bed,
                printer = printer,
                print = false,
                gcodeToolCount = 1,
                activeTools = intArrayOf(),
                firstToolUse = intArrayOf(),
                sourceFilamentTypes = emptyList(),
                pressureAdvance = SlicePreparationTools.PressureAdvanceCommands(emptyList(), emptyList()),
                activateProfile = SlicePrintPreparationDialog.PrinterProfileAction { true },
                reopenAfterReslice = {},
                sendAction = SliceMenuItems.SendAction { _, host, _, _, shouldPrint ->
                    sends.incrementAndGet()
                    sentHost.set(host)
                    printFlags.set(shouldPrint)
                },
            )
        }

        activity.runOnUiThread(::showPreparation)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("slice-preparation-title").assertIsDisplayed()
        compose.onNodeWithTag("slice-preparation-cancel").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Cancel must not dispatch upload or print", 0, sends.get())

        activity.runOnUiThread(::showPreparation)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("slice-preparation-confirm").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Only the injected test action should be called", 1, sends.get())
        assertEquals("http://printer.invalid", sentHost.get())
        assertEquals("The upload-only choice must never request a print start", false, printFlags.get())
        assertFalse(activity.isFinishing)
    }

    @Test
    fun emptyAceWarningKeepsUploadAndConfirmedPrintChoicesDistinct() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val action = AtomicReference<String?>()

        fun showWarning() {
            bed.showWorkspaceComposeDialog { dismiss ->
                PrintPreparationWarningDialog(
                    warning = PrintPreparationWarning.EmptyAceSlots(listOf(2)),
                    onDismiss = dismiss,
                    onAcknowledge = dismiss,
                    onUploadOnly = { action.set("upload-only"); dismiss() },
                    onFirmwareChecked = { action.set("firmware-checked-print"); dismiss() },
                )
            }
        }

        activity.runOnUiThread(::showWarning)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("ace-gap-cancel").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertNull("Cancel must not dispatch either upload or print", action.get())

        activity.runOnUiThread(::showWarning)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("ace-gap-upload-only").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("upload-only", action.get())

        action.set(null)
        activity.runOnUiThread(::showWarning)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("ace-gap-confirm-print").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Only the separate firmware-checked confirmation maps to the print branch",
            "firmware-checked-print", action.get())
        assertFalse("The test callback must not finish or print from the device", activity.isFinishing)
    }

    private fun createInitializedActivityRule() = run {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config
        createAndroidComposeRule<MainActivity>()
    }
}
