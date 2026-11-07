package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File
import kotlin.math.roundToInt

/** Generates a compact plate of personalized raised-text plant markers. */
object PlantLabelSetGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PlantTagsFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.PlantTagsTitle, onDismiss = dismiss) {
                PlantTagsFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = PlantTagsFormLogic.validate(submitted)
                    if (issue != null) error.value = issue.message
                    else {
                        dismiss()
                        generate(activity, bed, submitted)
                    }
                }
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, values: PlantTagsFormValues) {
        val labels = PlantTagsFormLogic.labels(values.names)
        val width = PlantTagsFormLogic.parse(values.width)
        val height = PlantTagsFormLogic.parse(values.height)
        val font = PlantTagsFormLogic.parse(values.font)
        val base = PlantTagsFormLogic.parse(values.base)
        val raised = PlantTagsFormLogic.parse(values.raised)
        val hole = PlantTagsFormLogic.parse(values.hole)
        val gap = PlantTagsFormLogic.parse(values.gap)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "plant-tags-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.PlantTagsPreparing))
            try {
                val columns = (width * 2.5f).roundToInt().coerceIn(96, 240)
                val rows = (height * 2.5f).roundToInt().coerceIn(48, 120)
                val surfaces = ArrayList<FloatArray>(labels.size)
                for (label in labels) {
                    surfaces.add(TextTagGenerator.rasterize(label, true, font, base, raised, hole,
                        columns, rows, width, height))
                }
                progress.setStage(25, activity.getString(R.string.PlantTagsBuilding))
                LithophaneStlWriter.writeTextTagBatch(output, surfaces, columns, rows, width, height, hole, gap)
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
