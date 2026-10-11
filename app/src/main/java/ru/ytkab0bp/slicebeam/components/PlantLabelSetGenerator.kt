package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File
import kotlin.math.roundToInt

/** Generates a compact plate of personalized raised-text plant markers. */
object PlantLabelSetGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PlantTagsFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        val content = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(activity).setTitle(R.string.PlantTagsTitle).setView(content)
            .setNegativeButton(android.R.string.cancel, null).create()
        content.setContent {
            BeamComposeTheme {
                PlantTagsFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = PlantTagsFormLogic.validate(submitted)
                    if (issue != null) error.value = issue.message
                    else {
                        dialog.dismiss()
                        generate(activity, bed, submitted)
                    }
                }
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ru.ytkab0bp.slicebeam.theme.ThemesRepo.getColor(android.R.attr.colorAccent),
            )
        }
        dialog.show()
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
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
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
                ViewUtils.postOnMainThread {
                    BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterFailed)
                        .setMessage(failure.message ?: failure.toString())
                        .setPositiveButton(android.R.string.ok, null).show()
                }
            }
        }
    }
}
