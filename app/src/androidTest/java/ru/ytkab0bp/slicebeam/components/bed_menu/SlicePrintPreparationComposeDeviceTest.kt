package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.PreSliceFilamentDialog
import ru.ytkab0bp.slicebeam.components.MmuAssignmentContent
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real workspace pre-send confirmation without issuing network or printer actions. */
@RunWith(AndroidJUnit4::class)
class SlicePrintPreparationComposeDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun selectingAnOccupiedAceGateSwapsTheVisibleToolAssignments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val mapping = mutableStateOf(listOf(0, 1, 2, 3))
        val palette = listOf(
            FilamentSlot(0xFF212721.toInt(), "PETG"),
            FilamentSlot(0xFFFFFFFF.toInt(), "PETG"),
            FilamentSlot(0xFF00BA4B.toInt(), "PETG"),
            FilamentSlot(0xFF0047BB.toInt(), "PLA"),
        )
        activity.runOnUiThread {
            bed.showWorkspaceComposeDialog {
                MmuAssignmentContent(
                    printerTitle = "synthetic ACE mapping test",
                    toolCount = 4,
                    loadedGates = intArrayOf(0, 1, 2, 3),
                    palette = palette,
                    mapping = mapping.value,
                    showHelp = false,
                    onMappingChange = { mapping.value = it },
                )
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }

        compose.onNodeWithTag("mmu-assignment-tool-1").assertIsDisplayed().performClick()
        compose.onNodeWithTag("mmu-assignment-gate-3").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("Choosing ACE 4 for T1 moves T3 to T1's former ACE 2 slot",
                listOf(0, 3, 2, 1), mapping.value)
        }
        compose.onNodeWithTag("mmu-assignment-tool-0").assertIsDisplayed()
    }

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

    @Test
    fun preSliceAssignmentCancelAndBackDoNotApplyAssignments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val selected = AtomicReference<IntArray?>()

        fun showAssignment() {
            PreSliceFilamentDialog.show(
                fragment = bed,
                model = null,
                callback = PreSliceFilamentDialog.Callback { selected.set(it) },
                positiveLabel = ru.ytkab0bp.slicebeam.R.string.ModelColorSlotsApply,
                titleLabel = ru.ytkab0bp.slicebeam.R.string.ModelColorSlotsAction,
            )
        }

        activity.runOnUiThread(::showAssignment)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
        compose.onNodeWithTag("pre-slice-cancel").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertNull("Cancel must not commit a filament assignment", selected.get())

        activity.runOnUiThread(::showAssignment)
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertNull("Workspace Back must not commit a filament assignment", selected.get())
        assertFalse(activity.isFinishing)
    }

    @Test
    fun sliceMenuPrinterActionsRequireSupportedHostAndDispatchOnlyInjectedCallback() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val config = SliceBeam.requireConfig()
        val printerTitle = requireNotNull(config.presets).get("printer")
        val printer = requireNotNull(config.findPrinter(printerTitle))
        val originalType = printer.get("host_type")
        val originalHost = printer.get("print_host")
        val originalKey = printer.get("printhost_apikey")
        val anchor = View(activity)
        val dispatched = mutableListOf<Triple<String, String, Boolean>>()

        fun actionsFor(type: String, host: String): List<BedComposeMenuAction> {
            printer.put("host_type", type)
            printer.put("print_host", host)
            printer.put("printhost_apikey", "synthetic-test-key")
            return SliceMenuItems.createAfterSlice(bed, SliceMenu()) { selectedType, selectedHost, _, _, print ->
                dispatched += Triple(selectedType, selectedHost, print)
            }
        }

        try {
            listOf("octoprint", "moonraker", "klipper").forEach { supportedType ->
                val actions = actionsFor(supportedType, "https://printer.invalid")
                val upload = actions.single { it.titleRes == R.string.MenuSliceSendToPrinter }
                val print = actions.single { it.titleRes == R.string.MenuSliceSendToPrinterAndPrint }
                upload.onClick(anchor)
                print.onClick(anchor)
            }
            assertEquals(
                listOf(
                    Triple("octoprint", "https://printer.invalid", false),
                    Triple("octoprint", "https://printer.invalid", true),
                    Triple("moonraker", "https://printer.invalid", false),
                    Triple("moonraker", "https://printer.invalid", true),
                    Triple("klipper", "https://printer.invalid", false),
                    Triple("klipper", "https://printer.invalid", true),
                ),
                dispatched,
            )

            assertTrue("unsupported host types must expose no printer actions",
                actionsFor("unsupported", "https://printer.invalid").none {
                    it.titleRes == R.string.MenuSliceSendToPrinter ||
                        it.titleRes == R.string.MenuSliceSendToPrinterAndPrint
                })
            assertTrue("blank printer hosts must expose no printer actions",
                actionsFor("moonraker", "").none {
                    it.titleRes == R.string.MenuSliceSendToPrinter ||
                        it.titleRes == R.string.MenuSliceSendToPrinterAndPrint
                })
            assertEquals("configuration filtering must not dispatch any hidden action", 6, dispatched.size)
        } finally {
            if (originalType == null) printer.values.remove("host_type") else printer.put("host_type", originalType)
            if (originalHost == null) printer.values.remove("print_host") else printer.put("print_host", originalHost)
            if (originalKey == null) printer.values.remove("printhost_apikey") else printer.put("printhost_apikey", originalKey)
        }
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
