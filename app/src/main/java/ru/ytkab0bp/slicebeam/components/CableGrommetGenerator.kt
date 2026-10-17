package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** On-device generator for a desk-mounted cable pass-through grommet. */
object CableGrommetGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(CableGrommetFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.CableGrommetTitle, onDismiss = dismiss) {
                CableGrommetFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = CableGrommetFormLogic.validate(submitted)
                    if (issue != null) error.value = issue.message
                    else {
                        dismiss()
                        generate(activity, bed, submitted)
                    }
                }
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: CableGrommetFormValues) {
        val diameter = CableGrommetFormLogic.parse(values.diameter)
        val clearance = CableGrommetFormLogic.parse(values.clearance)
        val wall = CableGrommetFormLogic.parse(values.wall)
        val flange = CableGrommetFormLogic.parse(values.flange)
        val flangeThickness = CableGrommetFormLogic.parse(values.flangeThickness)
        val tubeLength = CableGrommetFormLogic.parse(values.tubeLength)
        val screwHole = CableGrommetFormLogic.parse(values.screwHole)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "cable-grommet-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writeCableGrommet(
                    output, diameter, clearance, wall, flange, flangeThickness, tubeLength, screwHole,
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
