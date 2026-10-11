package ru.ytkab0bp.slicebeam.components.bed_menu

import ru.ytkab0bp.slicebeam.utils.GCodeToolMapper
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Locale

/** File and printer checks used while preparing a sliced job for upload. */
internal object SlicePreparationTools {
    private val supportedSendTypes = setOf("octoprint", "moonraker", "klipper")
    private val autoLevelCommand = Regex("(?is).*\\bG29\\b.*")

    fun isAnycubicKobra3Model(model: String?): Boolean =
        model?.lowercase(Locale.ROOT)?.startsWith("anycubic kobra 3") == true

    fun isSendTarget(hostType: String?, printHost: String?): Boolean =
        (hostType ?: "octoprint") in supportedSendTypes && !printHost.isNullOrEmpty()

    fun hasAutomaticBedLeveling(machineStartGcode: String?): Boolean =
        machineStartGcode?.matches(autoLevelCommand) == true

    fun didPrinterChange(originalTitle: String?, selectedTitle: String?): Boolean =
        selectedTitle != originalTitle

    enum class PrinterActivationResult { UNCHANGED, ACTIVATED, FAILED }

    /** A changed target must be active and saved before the job is re-sliced for it. */
    fun activateChangedPrinter(
        originalTitle: String?,
        selectedTitle: String?,
        activate: () -> Boolean,
    ): PrinterActivationResult {
        if (!didPrinterChange(originalTitle, selectedTitle)) return PrinterActivationResult.UNCHANGED
        return if (activate()) PrinterActivationResult.ACTIVATED else PrinterActivationResult.FAILED
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readMaxTool(gcode: File?): Int {
        if (gcode == null || !gcode.isFile || gcode.length() == 0L) {
            throw IOException("Sliced G-code is missing or empty")
        }
        var max = -1
        FileReader(gcode).buffered().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                max = maxOf(max, GCodeToolMapper.parseToolCommand(line))
            }
        }
        return max
    }

    /** Tools actually selected in the sliced file; T1-only jobs need no visible T0 row. */
    @JvmStatic
    @Throws(IOException::class)
    fun readUsedTools(gcode: File?): IntArray {
        if (gcode == null || !gcode.isFile || gcode.length() == 0L) {
            throw IOException("Sliced G-code is missing or empty")
        }
        val used = sortedSetOf<Int>()
        FileReader(gcode).buffered().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                GCodeToolMapper.parseToolCommand(line).takeIf { it >= 0 }?.let(used::add)
            }
        }
        return if (used.isEmpty()) intArrayOf(0) else used.toIntArray()
    }
}
