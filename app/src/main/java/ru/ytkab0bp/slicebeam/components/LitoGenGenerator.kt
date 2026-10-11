package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.launchAppActivityResult
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/** Independent multi-image LitoGEN cube workflow. Existing lithophane tools are left intact. */
object LitoGenGenerator {
    private var awaitingImages = false

    @JvmStatic
    fun pick(activity: Activity) {
        awaitingImages = true
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_IMAGE_TO_STL)
    }

    @JvmStatic
    fun isAwaitingImages(): Boolean = awaitingImages

    @JvmStatic
    fun cancel() {
        awaitingImages = false
    }

    @JvmStatic
    fun acceptPickedImages(activity: Activity, images: List<Uri>?, bed: BedFragment): Boolean {
        if (!awaitingImages) return false
        awaitingImages = false
        if (images == null || (images.size != 4 && images.size != 5)) {
            Toast.makeText(activity, R.string.LitoGenPickFourOrFive, Toast.LENGTH_LONG).show()
            return true
        }
        showOptions(activity, ArrayList(images), bed)
        return true
    }

    private fun showOptions(activity: Activity, images: List<Uri>, bed: BedFragment) {
        LitoGenOptionsDialog.show(activity, images) { options, saveAfter ->
            val validation = LitoGenFormLogic.validate(options)
            if (validation != null) {
                BeamAlertDialogBuilder(activity)
                    .setTitle(R.string.LitoGenTitle)
                    .setMessage(validationMessage(activity, validation))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                false
            } else {
                generate(activity, images, bed, options, saveAfter)
                true
            }
        }
    }

    private fun generate(
        activity: Activity,
        images: List<Uri>,
        bed: BedFragment,
        options: LitoGenOptions,
        saveAfter: Boolean,
    ) {
        val detail = intArrayOf(64, 96, 128, 192)[options.detailIndex]
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking))
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir, "litogen-photo-cube-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            val sideGrids = ArrayList<FloatArray>(4)
            try {
                val cols = ImageToStlConverter.gridColumns(detail, options.widthMm, options.heightMm)
                val rows = ImageToStlConverter.gridRows(detail, options.widthMm, options.heightMm)
                for (i in 0 until 4) {
                    val sideLabel = String.format(
                        Locale.getDefault(), activity.getString(R.string.LitoGenProcessingSide), i + 1
                    )
                    progress.setStage(8 + i * 13, sideLabel)
                    val image = ImageToStlConverter.decodeBitmap(activity.contentResolver, images[i], 512)
                    var cropped: Bitmap? = null
                    try {
                        cropped = centerCropToAspect(image, options.widthMm / options.heightMm)
                        sideGrids.add(
                            ImageToStlConverter.createTopZ(
                                cropped,
                                cols,
                                rows,
                                options.minThicknessMm,
                                options.maxThicknessMm,
                                3,
                                options.invert,
                                options.brightness / 100f,
                                options.contrast / 100f,
                                0,
                            ),
                        )
                    } finally {
                        image.recycle()
                        if (cropped != null && cropped !== image) cropped.recycle()
                    }
                }

                var lid: FloatArray? = null
                var lidCols = 0
                var lidRows = 0
                if (images.size == 5) {
                    progress.setStage(68, activity.getString(R.string.LitoGenProcessingLid))
                    val source = ImageToStlConverter.decodeBitmap(activity.contentResolver, images[4], 512)
                    var square: Bitmap? = null
                    try {
                        square = centerSquare(source)
                        lidCols = detail
                        lidRows = detail
                        lid = ImageToStlConverter.createTopZ(
                            square,
                            lidCols,
                            lidRows,
                            options.minThicknessMm,
                            options.maxThicknessMm,
                            3,
                            options.invert,
                            options.brightness / 100f,
                            options.contrast / 100f,
                            0,
                        )
                    } finally {
                        source.recycle()
                        if (square != null && square !== source) square.recycle()
                    }
                }

                progress.setStage(82, activity.getString(R.string.LitoGenPacking))
                LithophaneStlWriter.writeLitoGenCubeKit(
                    output,
                    sideGrids,
                    lid,
                    cols,
                    rows,
                    lidCols,
                    lidRows,
                    options.widthMm,
                    options.heightMm,
                    options.minThicknessMm,
                    3f,
                )
                progress.setStage(94, activity.getString(R.string.LitoGenValidating))
                progress.complete {
                    if (saveAfter) ImageToStlConverter.requestSave(activity, output, "stl", bed)
                    else ImageToStlConverter.saveGeneratedStl(activity, output, bed)
                }
            } catch (error: Exception) {
                progress.fail()
                if (output.exists()) output.delete()
                ViewUtils.postOnMainThread {
                    BeamAlertDialogBuilder(activity)
                        .setTitle(R.string.ConverterFailed)
                        .setMessage(error.message ?: error.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun validationMessage(activity: Activity, validation: LitoGenValidation): String =
        activity.getString(
            when (validation) {
                LitoGenValidation.SIZE -> R.string.LitoGenInvalidSize
                LitoGenValidation.THICKNESS -> R.string.LitoGenInvalidThickness
                LitoGenValidation.BRIGHTNESS -> R.string.LitoGenInvalidBrightness
                LitoGenValidation.CONTRAST -> R.string.LitoGenInvalidContrast
                LitoGenValidation.DETAIL -> R.string.LitoGenInvalidDetail
            },
        )

    internal fun imageLabel(activity: Activity, index: Int): String =
        if (index < 4) String.format(Locale.getDefault(), activity.getString(R.string.LitoGenSide), index + 1)
        else activity.getString(R.string.LitoGenLid)

    internal fun displayName(activity: Activity, uri: Uri): String {
        try {
            val cursor = activity.contentResolver.query(
                uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: activity.getString(R.string.LitoGenSelectedImage)
    }

    private fun centerSquare(source: Bitmap): Bitmap = centerCropToAspect(source, 1f)

    private fun centerCropToAspect(source: Bitmap, targetAspect: Float): Bitmap {
        val imageAspect = source.width / source.height.toFloat()
        var cropWidth = source.width
        var cropHeight = source.height
        if (imageAspect > targetAspect) cropWidth = maxOf(1, (cropHeight * targetAspect).roundToInt())
        else if (imageAspect < targetAspect) cropHeight = maxOf(1, (cropWidth / targetAspect).roundToInt())
        val crop = Bitmap.createBitmap(
            source, (source.width - cropWidth) / 2, (source.height - cropHeight) / 2, cropWidth, cropHeight
        )
        if (crop.width <= 512) return crop
        val scaled = Bitmap.createScaledBitmap(crop, 512, 512, true)
        if (scaled !== crop) crop.recycle()
        return scaled
    }
}
