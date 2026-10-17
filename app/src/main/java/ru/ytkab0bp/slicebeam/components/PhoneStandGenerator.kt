package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** Generates a one-piece desk stand for a phone or small tablet. */
object PhoneStandGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PhoneStandFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.PhoneStandTitle, onDismiss = dismiss) {
                PhoneStandFormContent(
                    values = values.value,
                    errorMessage = error.value,
                    onValuesChanged = {
                        values.value = it
                        error.value = null
                    },
                    onGenerate = { submitted ->
                        val issue = PhoneStandFormLogic.validate(submitted)
                        if (issue != null) {
                            error.value = issue.message
                        } else {
                            dismiss()
                            generate(activity, bed, submitted)
                        }
                    },
                )
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: PhoneStandFormValues) {
        val width = PhoneStandFormLogic.parse(values.width)
        val thickness = PhoneStandFormLogic.parse(values.deviceThickness)
        val tilt = PhoneStandFormLogic.parse(values.tilt)
        val depth = PhoneStandFormLogic.parse(values.depth)
        val back = PhoneStandFormLogic.parse(values.backHeight)
        val base = PhoneStandFormLogic.parse(values.baseThickness)
        val lip = PhoneStandFormLogic.parse(values.lipHeight)
        val cableGap = PhoneStandFormLogic.parse(values.cableGap)
        val cableDepth = PhoneStandFormLogic.parse(values.cableDepth)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "phone-stand-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writePhoneStand(
                    output, width, thickness, tilt, depth, back, base, lip, cableGap, cableDepth,
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
