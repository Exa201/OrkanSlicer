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

/** Generates a practical hollow box and optional matching lid locally. */
object UtilityBoxGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(UtilityBoxFormLogic.defaults())
        val content = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(activity).setTitle(R.string.UtilityBoxTitle).setView(content)
            .setNegativeButton(android.R.string.cancel, null).create()
        content.setContent {
            BeamComposeTheme {
                UtilityBoxFormContent(values.value, { values.value = it }) { submitted ->
                    dialog.dismiss()
                    generate(activity, bed, UtilityBoxFormLogic.normalized(submitted))
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

    private fun generate(activity: Activity, bed: BedFragment, values: UtilityBoxFormValues) {
        val width = UtilityBoxFormLogic.parse(values.width)
        val depth = UtilityBoxFormLogic.parse(values.depth)
        val height = UtilityBoxFormLogic.parse(values.height)
        val wall = UtilityBoxFormLogic.parse(values.wall)
        val floor = UtilityBoxFormLogic.parse(values.floor)
        val clearance = UtilityBoxFormLogic.parse(values.clearance)
        val lidSkirt = UtilityBoxFormLogic.parse(values.lidSkirt)
        val cableHole = UtilityBoxFormLogic.parse(values.cableHole)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
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
                ViewUtils.postOnMainThread {
                    BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterFailed)
                        .setMessage(failure.message ?: failure.toString())
                        .setPositiveButton(android.R.string.ok, null).show()
                }
            }
        }
    }
}
