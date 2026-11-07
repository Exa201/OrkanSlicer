package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File

/** Locally generates a tapered planter and an optional matching drip saucer. */
object PlanterPotGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PlanterPotFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.PlanterTitle, onDismiss = dismiss) {
                PlanterPotFormContent(
                    values = values.value,
                    errorMessage = error.value,
                    onValuesChanged = {
                        values.value = it
                        error.value = null
                    },
                    onGenerate = { submitted ->
                        val issue = PlanterPotFormLogic.validate(submitted)
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

    private fun generate(activity: Activity, bed: BedFragment, values: PlanterPotFormValues) {
        val bottom = PlanterPotFormLogic.parse(values.bottomDiameter)
        val top = PlanterPotFormLogic.parse(values.topDiameter)
        val height = PlanterPotFormLogic.parse(values.height)
        val wall = PlanterPotFormLogic.parse(values.wall)
        val base = PlanterPotFormLogic.parse(values.base)
        val drain = PlanterPotFormLogic.parse(values.drain)
        val clearance = PlanterPotFormLogic.parse(values.saucerClearance)
        val saucerWall = PlanterPotFormLogic.parse(values.saucerWall)
        val saucerHeight = PlanterPotFormLogic.parse(values.saucerHeight)
        val saucerBase = PlanterPotFormLogic.parse(values.saucerBase)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "planter-pot-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                LithophaneStlWriter.writePlanterPot(
                    output, bottom, top, height, wall, base, drain, values.withSaucer,
                    clearance, saucerWall, saucerHeight, saucerBase,
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
