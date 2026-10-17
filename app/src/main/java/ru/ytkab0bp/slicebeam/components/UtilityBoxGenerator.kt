package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** Generates a practical hollow box and optional matching lid locally. */
object UtilityBoxGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(UtilityBoxFormLogic.defaults())
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.UtilityBoxTitle, onDismiss = dismiss) {
                UtilityBoxFormContent(values.value, { values.value = it }) { submitted ->
                    dismiss()
                    generate(activity, bed, UtilityBoxFormLogic.normalized(submitted))
                }
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: UtilityBoxFormValues) {
        val width = UtilityBoxFormLogic.parse(values.width)
        val depth = UtilityBoxFormLogic.parse(values.depth)
        val height = UtilityBoxFormLogic.parse(values.height)
        val wall = UtilityBoxFormLogic.parse(values.wall)
        val floor = UtilityBoxFormLogic.parse(values.floor)
        val clearance = UtilityBoxFormLogic.parse(values.clearance)
        val lidSkirt = UtilityBoxFormLogic.parse(values.lidSkirt)
        val cableHole = UtilityBoxFormLogic.parse(values.cableHole)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "utility-box-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writeUtilityBox(
                    output, width, depth, height, wall, floor, clearance, lidSkirt, cableHole, values.withLid,
                )
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
