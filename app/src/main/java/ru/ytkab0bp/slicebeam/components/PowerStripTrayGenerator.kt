package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** Builds a low-profile under-desk tray for a power strip. */
object PowerStripTrayGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PowerStripFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.PowerStripTitle, onDismiss = dismiss) {
                PowerStripFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = PowerStripFormLogic.validate(submitted)
                    if (issue != null) error.value = issue.message
                    else {
                        dismiss()
                        generate(activity, bed, submitted)
                    }
                }
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: PowerStripFormValues) {
        val length = PowerStripFormLogic.parse(values.length)
        val width = PowerStripFormLogic.parse(values.width)
        val height = PowerStripFormLogic.parse(values.height)
        val clearance = PowerStripFormLogic.parse(values.clearance)
        val base = PowerStripFormLogic.parse(values.base)
        val stop = PowerStripFormLogic.parse(values.stop)
        val notch = PowerStripFormLogic.parse(values.notch)
        val screw = PowerStripFormLogic.parse(values.screw)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "power-strip-tray-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writePowerStripTray(output, length, width, height, clearance, base, stop, notch, screw)
                progress.setStage(94, activity.getString(R.string.FastenerChecking))
                progress.complete { ImageToStlConverter.saveGeneratedStl(activity, output, bed) }
            } catch (failure: Exception) {
                progress.fail()
                if (output.exists()) output.delete()
                showGeneratorFailure(activity, bed, failure.message ?: failure.toString())
            }
        }
    }
}
