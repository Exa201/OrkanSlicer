package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** Makes a printable multi-tool wall holder with screw holes. */
object WallToolHolderGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(WallToolHolderFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.WallToolHolderTitle, onDismiss = dismiss) {
                WallToolHolderFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = WallToolHolderFormLogic.validate(submitted)
                    if (issue != null) error.value = issue.message
                    else {
                        dismiss()
                        generate(activity, bed, submitted)
                    }
                }
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: WallToolHolderFormValues) {
        val diameter = WallToolHolderFormLogic.parse(values.diameter)
        val clearance = WallToolHolderFormLogic.parse(values.clearance)
        val count = WallToolHolderFormLogic.parseCount(values.count)
        val thickness = WallToolHolderFormLogic.parse(values.thickness)
        val screw = WallToolHolderFormLogic.parse(values.screw)
        val guideHeight = WallToolHolderFormLogic.parse(values.guideHeight)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "wall-tool-holder-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writeWallToolHolder(output, diameter, clearance, count, thickness, screw, guideHeight)
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
