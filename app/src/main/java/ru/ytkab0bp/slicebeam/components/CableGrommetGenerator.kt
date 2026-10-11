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

/** On-device generator for a desk-mounted cable pass-through grommet. */
object CableGrommetGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(CableGrommetFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        val content = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(activity).setTitle(R.string.CableGrommetTitle).setView(content)
            .setNegativeButton(android.R.string.cancel, null).create()
        content.setContent {
            BeamComposeTheme {
                CableGrommetFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                    val issue = CableGrommetFormLogic.validate(submitted)
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

    private fun generate(activity: Activity, bed: BedFragment, values: CableGrommetFormValues) {
        val diameter = CableGrommetFormLogic.parse(values.diameter)
        val clearance = CableGrommetFormLogic.parse(values.clearance)
        val wall = CableGrommetFormLogic.parse(values.wall)
        val flange = CableGrommetFormLogic.parse(values.flange)
        val flangeThickness = CableGrommetFormLogic.parse(values.flangeThickness)
        val tubeLength = CableGrommetFormLogic.parse(values.tubeLength)
        val screwHole = CableGrommetFormLogic.parse(values.screwHole)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
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
                ViewUtils.postOnMainThread {
                    BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterFailed)
                        .setMessage(failure.message ?: failure.toString())
                        .setPositiveButton(android.R.string.ok, null).show()
                }
            }
        }
    }
}
