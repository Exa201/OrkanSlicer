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

/** Generates a one-piece desk stand for a phone or small tablet. */
object PhoneStandGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        val values = mutableStateOf(PhoneStandFormLogic.defaults())
        val error = mutableStateOf<Int?>(null)
        val content = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }
        val dialog = BeamAlertDialogBuilder(activity)
            .setTitle(R.string.PhoneStandTitle)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        content.setContent {
            BeamComposeTheme {
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
                            dialog.dismiss()
                            generate(activity, bed, submitted)
                        }
                    },
                )
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
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
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
                ViewUtils.postOnMainThread {
                    BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterFailed)
                        .setMessage(failure.message ?: failure.toString())
                        .setPositiveButton(android.R.string.ok, null).show()
                }
            }
        }
    }
}
