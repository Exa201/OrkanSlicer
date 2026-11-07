package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** On-device raised text nameplate/key-tag generator. */
class TextTagGenerator private constructor() {
    companion object {
        @JvmStatic
        fun show(activity: Activity, bed: BedFragment) {
            val values = mutableStateOf(TextTagFormLogic.defaults())
            val error = mutableStateOf<Int?>(null)
            bed.showWorkspaceComposeDialog { dismiss ->
                GeneratorWorkspaceDialog(R.string.TextTagTitle, onDismiss = dismiss) {
                    TextTagFormContent(values.value, error.value, { values.value = it; error.value = null }) { submitted ->
                        val issue = TextTagFormLogic.validate(submitted)
                        if (issue != null) error.value = issue.message
                        else {
                            dismiss()
                            generate(activity, bed, TextTagFormLogic.normalized(submitted))
                        }
                    }
                }
            }
        }

        private fun generate(activity: Activity, bed: BedFragment, values: TextTagFormValues) {
            val text = values.text.trim()
            val width = TextTagFormLogic.parse(values.width)
            val height = TextTagFormLogic.parse(values.height)
            val fontSize = TextTagFormLogic.parse(values.fontSize)
            val base = TextTagFormLogic.parse(values.baseThickness)
            val raised = TextTagFormLogic.parse(values.raisedHeight)
            val hole = TextTagFormLogic.parse(values.holeDiameter)
            val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
            IOUtils.IO_POOL.execute {
                val output = File(activity.cacheDir, "text-tag-${System.currentTimeMillis()}.stl")
                progress.setOutputFile(output)
                progress.setStage(12, activity.getString(R.string.FastenerBuilding))
                try {
                    val columns = max(64, min(320, (width * 3f).roundToInt()))
                    val rows = max(48, min(240, (height * 3f).roundToInt()))
                    val topZ = rasterize(text, values.bold, fontSize, base, raised, hole, columns, rows, width, height)
                    LithophaneStlWriter.writeTextTag(output, topZ, columns, rows, width, height, hole)
                    progress.setStage(94, activity.getString(R.string.FastenerChecking))
                    progress.complete { ImageToStlConverter.saveGeneratedStl(activity, output, bed) }
                } catch (failure: Exception) {
                    progress.fail()
                    if (output.exists()) output.delete()
                    showGeneratorFailure(activity, bed, failure.message ?: failure.toString())
                }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun rasterize(
            text: String, bold: Boolean, fontMm: Float, baseMm: Float,
            raisedMm: Float, holeMm: Float, cols: Int, rows: Int,
            widthMm: Float, heightMm: Float,
        ): FloatArray {
            val mask = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(mask)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                    color = Color.BLACK
                    typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
                }
                val pxPerMm = min((cols - 1f) / widthMm, (rows - 1f) / heightMm)
                var fontPx = min(fontMm * pxPerMm, (rows - 1f) * 0.72f)
                val safeWidthMm = max(8f, widthMm - if (holeMm > 0f) holeMm + 10f else 8f)
                val maxTextWidth = max(1f, safeWidthMm * pxPerMm)
                paint.textSize = fontPx
                val measured = paint.measureText(text)
                if (measured > maxTextWidth) {
                    fontPx *= maxTextWidth / measured
                    paint.textSize = fontPx
                }
                val metrics = paint.fontMetrics
                val baseline = rows / 2f - (metrics.ascent + metrics.descent) / 2f
                val x = max(1f, (safeWidthMm * pxPerMm - paint.measureText(text)) / 2f)
                canvas.drawText(text, x, baseline, paint)
                val pixels = IntArray(cols * rows)
                mask.getPixels(pixels, 0, cols, 0, 0, cols, rows)
                val topZ = FloatArray(pixels.size)
                for (index in pixels.indices) {
                    val ink = 1f - Color.red(pixels[index]) / 255f
                    topZ[index] = max(0.4f, baseMm + ink * raisedMm)
                }
                return topZ
            } finally {
                mask.recycle()
            }
        }
    }
}
