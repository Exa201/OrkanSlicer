package ru.ytkab0bp.slicebeam.components
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.launchAppActivityResult
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.ConverterStorage
import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter
import ru.ytkab0bp.slicebeam.utils.LithophaneToneMapper
import ru.ytkab0bp.slicebeam.utils.ViewUtils
/** Local image converter. Currently supports raster images to STL reliefs or SVG silhouettes. */
object ImageToStlConverter {
private val MAX_SIDE = 192
private val MAX_VECTOR_SIDE = 128
private var pendingOutput: File? = null
private var pendingExtension: String? = null
private var requestedMode:Int = 0
private var requestedSourceFormat = -1
private var requestedTargetFormat = -1
private var pendingBed: WeakReference<BedFragment?> = WeakReference(null)
private val cubeImageSelection = ArrayList<Uri>(4)
private var selectingCubeImages:Boolean = false
@JvmStatic
fun pickImage(activity:Activity) {
showFormatPairPicker(activity, null)
}
@JvmStatic
fun pickImage(activity:Activity, bed:BedFragment?) {
showFormatPairPicker(activity, bed)
}
/** Pick a supported local image conversion pair before choosing its source file. */
private fun showFormatPairPicker(activity:Activity, bed:BedFragment?) {
if (bed?.canShowWorkspaceComposeDialog() == true) {
    bed.showWorkspaceComposeDialog { dismiss ->
        ImageFormatPairDialog(
            onDismiss = dismiss,
            onChooseSource = { sourceFormat, targetFormat ->
                requestedSourceFormat = sourceFormat
                requestedTargetFormat = targetFormat
                dismiss()
                pickSelectedSource(activity, sourceFormat)
            },
        )
    }
    return
}
val form = LinearLayout(activity)
form.setOrientation(LinearLayout.VERTICAL)
form.setPadding(ViewUtils.dp(24), ViewUtils.dp(8), ViewUtils.dp(24), ViewUtils.dp(4))
val sourceLabel = TextView(activity)
sourceLabel.setText(R.string.ConverterSourceFormat)
form.addView(sourceLabel)
val source = Spinner(activity)
source.setAdapter(ArrayAdapter.createFromResource(activity, R.array.ConverterSources,
android.R.layout.simple_spinner_dropdown_item))
form.addView(source)
val targetLabel = TextView(activity)
targetLabel.setText(R.string.ConverterTarget)
form.addView(targetLabel)
val target = Spinner(activity)
target.setAdapter(ArrayAdapter.createFromResource(activity, R.array.ConverterTargets,
android.R.layout.simple_spinner_dropdown_item))
form.addView(target)
val note = TextView(activity)
note.setText(R.string.ConverterLocalFormatsNote)
form.addView(note)
BeamAlertDialogBuilder(activity)
.setTitle(R.string.ConverterTitle)
.setView(form)
.setNegativeButton(android.R.string.cancel, null)
.setPositiveButton(R.string.ConverterChooseSourceFile, { dialog, which->
requestedSourceFormat = source.getSelectedItemPosition()
requestedTargetFormat = target.getSelectedItemPosition()
pickSelectedSource(activity, requestedSourceFormat) }).show()
}
private fun pickSelectedSource(activity:Activity, format:Int) {
val mime = arrayOf<String>("image/png", "image/jpeg", "image/webp")[Math.max(0, Math.min(2, format))]
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
intent.addCategory(Intent.CATEGORY_OPENABLE)
intent.setType(mime)
intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_IMAGE_TO_STL)
}
@JvmStatic
fun pickLithophane(activity:Activity) {
pickImages(activity, 2)
}
@JvmStatic
fun pickLithophaneCube(activity:Activity) {
requestedMode = 3
selectingCubeImages = true
cubeImageSelection.clear()
pickNextCubeImage(activity)
}
@JvmStatic
fun pickLithophaneFrame(activity:Activity) {
pickImages(activity, 4)
}
@JvmStatic
fun pickCurvedLithophane(activity:Activity) {
pickImages(activity, 5)
}
private fun pickImages(activity:Activity, initialMode:Int) {
requestedMode = initialMode
selectingCubeImages = false
cubeImageSelection.clear()
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
intent.addCategory(Intent.CATEGORY_OPENABLE)
intent.setType("image/*")
intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_IMAGE_TO_STL)
}
private fun pickNextCubeImage(activity:Activity) {
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
intent.addCategory(Intent.CATEGORY_OPENABLE)
intent.setType("image/*")
intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_IMAGE_TO_STL)
}
@JvmStatic
fun onImagePickerCancelled() {
LitoGenGenerator.cancel()
selectingCubeImages = false
cubeImageSelection.clear()
requestedSourceFormat = -1
requestedTargetFormat = -1
}
@JvmStatic
fun handlePickedImages(activity:Activity, images:List<Uri>, bed:BedFragment) {
if (LitoGenGenerator.acceptPickedImages(activity, images, bed)) return
if (!selectingCubeImages && requestedSourceFormat >= 0 && !images.isEmpty())
{
val actual = detectedSourceFormat(activity, images.get(0))
if (actual != requestedSourceFormat)
{
requestedSourceFormat = -1
requestedTargetFormat = -1
Toast.makeText(activity, R.string.ConverterSourceFormatMismatch, Toast.LENGTH_LONG).show()
return
}
}
if (!selectingCubeImages)
{
showOptions(activity, images, bed)
return
}
for (image in images)
{
if (image != null && cubeImageSelection.size < 4) cubeImageSelection.add(image)
}
if (cubeImageSelection.size == 4)
{
selectingCubeImages = false
val completed = ArrayList<Uri>(cubeImageSelection)
cubeImageSelection.clear()
showOptions(activity, completed, bed)
}
else
{
Toast.makeText(activity, activity.getString(R.string.LithophaneCubeImageProgress,
cubeImageSelection.size + 1, 4), Toast.LENGTH_LONG).show()
pickNextCubeImage(activity)
}
}
private fun detectedSourceFormat(activity:Activity, uri:Uri):Int {
val mime = activity.getContentResolver().getType(uri)
if ("image/png".equals(mime, true)) return 0
if ("image/jpeg".equals(mime, true) || "image/jpg".equals(mime, true)) return 1
if ("image/webp".equals(mime, true)) return 2
var name = if (uri == null) "" else java.lang.String.valueOf(uri.getLastPathSegment())
try
{
activity.getContentResolver().query(uri,
arrayOf<String>(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null).use({ cursor-> if (cursor != null && cursor.moveToFirst())
{
val displayName = cursor.getString(0)
if (displayName != null) name = displayName
} })
}
catch (ignored:Exception) {}
name = name.lowercase(java.util.Locale.ROOT)
if (name.endsWith(".png")) return 0
if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return 1
if (name.endsWith(".webp")) return 2
return -1
}
@JvmStatic
fun showOptions(activity:Activity, image:Uri, bed:BedFragment) {
val images = ArrayList<Uri>()
if (image != null) images.add(image)
showOptions(activity, images, bed)
}
@JvmStatic
fun showOptions(activity:Activity, selectedImages:List<Uri>, bed:BedFragment) {
val images = ArrayList(selectedImages)
val initialMode = requestedMode
requestedMode = 0
val initialTarget = requestedTargetFormat.takeIf { it in 0..4 } ?: 0
requestedSourceFormat = -1
requestedTargetFormat = -1
if (bed.canShowWorkspaceComposeDialog()) {
    showWorkspaceOptions(activity, images, bed, defaultImageToStlOptions(initialMode, initialTarget))
    return
}
showOptionsLegacy(activity, images, bed, initialMode, initialTarget)
}
private fun showWorkspaceOptions(
    activity: Activity,
    images: List<Uri>,
    bed: BedFragment,
    initialValues: ImageToStlOptions,
) {
    val formState = androidx.compose.runtime.mutableStateOf(initialValues)
    lateinit var reopenForm: () -> Unit
    reopenForm = {
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.ConverterTitle, onDismiss = dismiss) {
                ImageToStlOptionsForm(
                    activity = activity,
                    images = images,
                    values = formState.value,
                    onValuesChanged = { formState.value = it },
                    onEditImage = {
                        val image = images.firstOrNull() ?: return@ImageToStlOptionsForm
                        dismiss()
                        val savedTone = intArrayOf(formState.value.toneMode, formState.value.brightness, formState.value.contrast)
                        val savedInvert = booleanArrayOf(formState.value.invert)
                        showLithophaneImageEditor(activity, image, bed, savedTone, savedInvert) {
                            formState.value = formState.value.copy(
                                toneMode = savedTone[0], brightness = savedTone[1],
                                contrast = savedTone[2], invert = savedInvert[0],
                            )
                            // External Back dismissal calls this before the workspace clears the
                            // editor panel. Reopen on the next UI turn so the editor's cleanup
                            // cannot clear the newly attached parent form.
                            activity.window.decorView.post {
                                if (!activity.isFinishing && !activity.isDestroyed) reopenForm()
                            }
                        }
                    },
                    onGenerate = { values, saveAfterGeneration ->
                        formState.value = values
                        dismiss()
                        generateImageToStl(activity, images, bed, values, saveAfterGeneration)
                    },
                )
            }
        }
    }
    reopenForm()
}

private fun showOptionsLegacy(
    activity: Activity,
    images: ArrayList<Uri>,
    bed: BedFragment,
    initialMode: Int,
    initialTarget: Int,
) {
val form = LinearLayout(activity)
form.setOrientation(LinearLayout.VERTICAL)
form.setPadding(ViewUtils.dp(24), ViewUtils.dp(4), ViewUtils.dp(24), ViewUtils.dp(4))
val targetLabel = TextView(activity)
targetLabel.setText(R.string.ConverterTarget)
form.addView(targetLabel)
val target = Spinner(activity)
target.setAdapter(ArrayAdapter.createFromResource(activity, R.array.ConverterTargets,
android.R.layout.simple_spinner_dropdown_item))
if (initialTarget in 0 until target.getCount()) target.setSelection(initialTarget)
form.addView(target)
val modeLabel = TextView(activity)
modeLabel.setText(R.string.ImageToStlMode)
form.addView(modeLabel)
val mode = Spinner(activity)
mode.setAdapter(ArrayAdapter.createFromResource(activity, R.array.ImageToStlModes,
android.R.layout.simple_spinner_dropdown_item))
if (initialMode >= 0 && initialMode < 11) mode.setSelection(initialMode)
form.addView(mode)
val selectionSummary = TextView(activity)
selectionSummary.setText(activity.getResources().getQuantityString(
R.plurals.LithophaneImagesSelected, images.size, images.size))
form.addView(selectionSummary)
val sourcePreview = ImageView(activity)
sourcePreview.setScaleType(ImageView.ScaleType.FIT_CENTER)
sourcePreview.setBackgroundColor(Color.rgb(38, 40, 44))
sourcePreview.setContentDescription(activity.getString(R.string.ConverterSourcePreview))
form.addView(sourcePreview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
ViewUtils.dp(180)))
if (!images.isEmpty())
{
val previewUri = images.get(0)
Thread({ var previewBitmap:Bitmap? = null
try
{
previewBitmap = decodeBitmap(activity.getContentResolver(), previewUri, 512)
}
catch (ignored:Exception) {}
val result = previewBitmap
if (result != null && !activity.isFinishing())
activity.runOnUiThread({ if (!sourcePreview.isAttachedToWindow()) {
result.recycle()
return@runOnUiThread
}
sourcePreview.setImageBitmap(result) }) }, "ConverterPreview").start()
}
val imageToneSettings = intArrayOf(0, 100, 100)
val invertImage = booleanArrayOf(false)
lateinit var optionsDialog: androidx.appcompat.app.AlertDialog
val editImage = android.widget.Button(activity)
editImage.setText(R.string.LithophaneImageEditorOpen)
form.addView(editImage, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
ViewUtils.dp(48)))
editImage.setOnClickListener({ v->
if (images.isEmpty())
{
Toast.makeText(activity, R.string.ConverterPickImage, Toast.LENGTH_LONG).show()
return@setOnClickListener
}
optionsDialog.dismiss()
showLithophaneImageEditor(activity, images.get(0), bed, imageToneSettings, invertImage) {
    if (!activity.isFinishing && !activity.isDestroyed) optionsDialog.show()
}
})
val width = addNumberField(activity, form, R.string.ImageToStlWidth, if (initialMode >= 2) "45" else "80")
val height = addNumberField(activity, form, R.string.ImageToStlHeight, "60")
val baseLabel = TextView(activity)
baseLabel.setText(R.string.ImageToStlBase)
form.addView(baseLabel)
val base = addNumberField(activity, form, R.string.ImageToStlBase, "1.2", false)
val reliefLabel = TextView(activity)
reliefLabel.setText(R.string.ImageToStlReliefHeight)
form.addView(reliefLabel)
val relief = addNumberField(activity, form, R.string.ImageToStlReliefHeight, "4.0", false)
val resolutionLabel = TextView(activity)
resolutionLabel.setText(R.string.LithophaneResolution)
form.addView(resolutionLabel)
val resolution = Spinner(activity)
resolution.setAdapter(ArrayAdapter.createFromResource(activity, R.array.LithophaneResolutions,
android.R.layout.simple_spinner_dropdown_item))
form.addView(resolution)
val frameBaseLabel = TextView(activity)
frameBaseLabel.setText(R.string.LithophaneFrameBaseDepth)
form.addView(frameBaseLabel)
val frameBaseDepth = addNumberField(activity, form, R.string.LithophaneFrameBaseDepth, "5", false)
val footLabel = TextView(activity)
footLabel.setText(R.string.LithophaneFrameFootDepth)
form.addView(footLabel)
val footDepth = addNumberField(activity, form, R.string.LithophaneFrameFootDepth, "24", false)
val lightOpeningLabel = TextView(activity)
lightOpeningLabel.setText(R.string.LithophaneFrameOpening)
form.addView(lightOpeningLabel)
val lightOpening = addNumberField(activity, form, R.string.LithophaneFrameOpening, "18", false)
val curveRadiusLabel = TextView(activity)
curveRadiusLabel.setText(R.string.LithophaneCurveRadius)
form.addView(curveRadiusLabel)
val curveRadius = addNumberField(activity, form, R.string.LithophaneCurveRadius, "55", false)
val roundHelp = TextView(activity)
roundHelp.setText(R.string.LithophaneRoundDescription)
form.addView(roundHelp)
val diameterLabel = TextView(activity)
diameterLabel.setText(R.string.LithophaneBaseDiameter)
form.addView(diameterLabel)
val diameter = addNumberField(activity, form, R.string.LithophaneBaseDiameter, "90", false)
val topDiameterLabel = TextView(activity)
topDiameterLabel.setText(R.string.LithophaneTopDiameter)
form.addView(topDiameterLabel)
val topDiameter = addNumberField(activity, form, R.string.LithophaneTopDiameter, "90", false)
val angleLabel = TextView(activity)
angleLabel.setText(R.string.LithophaneWrapAngle)
form.addView(angleLabel)
val wrapAngle = android.widget.SeekBar(activity)
wrapAngle.setMax(360)
wrapAngle.setProgress(360)
form.addView(wrapAngle)
val waveLabel = TextView(activity)
waveLabel.setText(R.string.LithophaneWaves)
form.addView(waveLabel)
val waveCount = android.widget.SeekBar(activity)
waveCount.setMax(24)
form.addView(waveCount)
val waveDepth = addNumberField(activity, form, R.string.LithophaneWaveDepth, "3", true)
val closeBottom = CheckBox(activity)
closeBottom.setText(R.string.LithophaneCloseBottom)
closeBottom.setChecked(true)
form.addView(closeBottom)
val closeTop = CheckBox(activity)
closeTop.setText(R.string.LithophaneCloseTop)
form.addView(closeTop)
val createLightBase = CheckBox(activity)
createLightBase.setText(R.string.LithophaneCreateBase)
form.addView(createLightBase)
val baseThicknessInput = addNumberField(activity, form, R.string.LithophaneBaseThickness, "7.5", true)
val cableDiameterInput = addNumberField(activity, form, R.string.LithophaneCableDiameter, "6", true)
val frameWidthLabel = TextView(activity)
frameWidthLabel.setText(R.string.LithophaneFrameWidth)
form.addView(frameWidthLabel)
val frameWidth = addNumberField(activity, form, R.string.LithophaneFrameWidth, "4", false)
updateCurvedOptions(mode.getSelectedItemPosition(), target.getSelectedItemPosition(), curveRadiusLabel, curveRadius)
updateRoundOptions(mode.getSelectedItemPosition(), target.getSelectedItemPosition(), roundHelp,
diameterLabel, diameter, topDiameterLabel, topDiameter, angleLabel, wrapAngle,
waveLabel, waveCount, waveDepth, closeBottom, closeTop, createLightBase,
baseThicknessInput, cableDiameterInput, frameWidthLabel, frameWidth)
target.setOnItemSelectedListener(object:android.widget.AdapterView.OnItemSelectedListener {
override fun onItemSelected(parent:android.widget.AdapterView<*>, view:android.view.View, position:Int, id:Long) {
val modelOptions = if (position == 0) android.view.View.VISIBLE else android.view.View.GONE
val imageOptions = if (position <= 1) android.view.View.VISIBLE else android.view.View.GONE
modeLabel.setVisibility(imageOptions)
mode.setVisibility(imageOptions)
selectionSummary.setVisibility(modelOptions)
val showPreview = position == 0
editImage.setVisibility(if (showPreview) View.VISIBLE else View.GONE)
updateGeneratorOptions(mode.getSelectedItemPosition(), modelOptions, width, height,
baseLabel, base, reliefLabel, relief, resolutionLabel, resolution)
updateFrameOptions(mode.getSelectedItemPosition(), modelOptions, frameBaseLabel, frameBaseDepth,
footLabel, footDepth, lightOpeningLabel, lightOpening)
updateCurvedOptions(mode.getSelectedItemPosition(), position, curveRadiusLabel, curveRadius)
updateRoundOptions(mode.getSelectedItemPosition(), position, roundHelp, diameterLabel, diameter,
topDiameterLabel, topDiameter, angleLabel, wrapAngle, waveLabel, waveCount, waveDepth,
closeBottom, closeTop, createLightBase, baseThicknessInput, cableDiameterInput, frameWidthLabel, frameWidth)
}
override fun onNothingSelected(parent:android.widget.AdapterView<*>) {}
})
mode.setOnItemSelectedListener(object:android.widget.AdapterView.OnItemSelectedListener {
override fun onItemSelected(parent:android.widget.AdapterView<*>, view:android.view.View, position:Int, id:Long) {
updateGeneratorOptions(position, if (target.getSelectedItemPosition() == 0) android.view.View.VISIBLE else android.view.View.GONE,
width, height, baseLabel, base, reliefLabel, relief, resolutionLabel, resolution)
updateFrameOptions(position, if (target.getSelectedItemPosition() == 0) android.view.View.VISIBLE else android.view.View.GONE,
frameBaseLabel, frameBaseDepth, footLabel, footDepth, lightOpeningLabel, lightOpening)
updateCurvedOptions(position, target.getSelectedItemPosition(), curveRadiusLabel, curveRadius)
updateRoundOptions(position, target.getSelectedItemPosition(), roundHelp, diameterLabel, diameter,
topDiameterLabel, topDiameter, angleLabel, wrapAngle, waveLabel, waveCount, waveDepth,
closeBottom, closeTop, createLightBase, baseThicknessInput, cableDiameterInput, frameWidthLabel, frameWidth)
}
override fun onNothingSelected(parent:android.widget.AdapterView<*>) {}
})
val scroll = ScrollView(activity)
scroll.setFillViewport(true)
scroll.addView(form, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.WRAP_CONTENT))
val dialogHeight = Math.min(ViewUtils.dp(480), activity.getResources().getDisplayMetrics().heightPixels * 2 / 3)
scroll.setLayoutParams(ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight))
val dialogContent = LinearLayout(activity)
dialogContent.setOrientation(LinearLayout.VERTICAL)
dialogContent.addView(scroll, LinearLayout.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight))
val generate = { saveAfterGeneration: Boolean ->
val targetFormat = target.getSelectedItemPosition().coerceIn(0, 4)
val selectedMode = mode.getSelectedItemPosition().let { if (targetFormat != 0 && it > 1) 2 else it.coerceIn(0, 10) }
generateImageToStl(
    activity,
    images,
    bed,
    ImageToStlOptions(
        targetFormat = targetFormat,
        mode = selectedMode,
        width = width.getText().toString(),
        height = height.getText().toString(),
        base = base.getText().toString(),
        relief = relief.getText().toString(),
        resolution = resolution.getSelectedItemPosition().coerceIn(0, 3),
        frameBase = frameBaseDepth.getText().toString(),
        footDepth = footDepth.getText().toString(),
        openingDiameter = lightOpening.getText().toString(),
        curveRadius = curveRadius.getText().toString(),
        diameter = diameter.getText().toString(),
        topDiameter = topDiameter.getText().toString(),
        wrapAngle = wrapAngle.getProgress(),
        waveCount = waveCount.getProgress(),
        waveDepth = waveDepth.getText().toString(),
        closeBottom = closeBottom.isChecked(),
        closeTop = closeTop.isChecked(),
        createLightBase = createLightBase.isChecked(),
        lightBaseThickness = baseThicknessInput.getText().toString(),
        cableDiameter = cableDiameterInput.getText().toString(),
        frameWidth = frameWidth.getText().toString(),
        toneMode = imageToneSettings[0],
        brightness = imageToneSettings[1],
        contrast = imageToneSettings[2],
        invert = invertImage[0],
    ),
    saveAfterGeneration,
)
}
val generateButton = android.widget.Button(activity)
generateButton.setText(R.string.LithophaneGenerate)
val generateAndSaveButton = android.widget.Button(activity)
generateAndSaveButton.setText(R.string.LithophaneGenerateAndSave)
val actionParams = LinearLayout.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(48))
actionParams.setMargins(ViewUtils.dp(16), ViewUtils.dp(2), ViewUtils.dp(16), ViewUtils.dp(2))
dialogContent.addView(generateButton, actionParams)
dialogContent.addView(generateAndSaveButton, actionParams)
optionsDialog = BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterTitle)
.setMessage(R.string.LithophaneGeneratorDescription).setView(dialogContent)
.setNegativeButton(android.R.string.cancel, null)
.create()
optionsDialog.show()
generateButton.setOnClickListener({ v->
optionsDialog.dismiss()
generate(false) })
generateAndSaveButton.setOnClickListener({ v->
optionsDialog.dismiss()
generate(true) })
}
private fun generateImageToStl(
    activity: Activity,
    images: List<Uri>,
    bed: BedFragment,
    options: ImageToStlOptions,
    saveAfterGeneration: Boolean,
) {
 val targetFormat = options.targetFormat.coerceIn(0, 4)
val extension = arrayOf<String>("stl", "svg", "png", "jpg", "webp")[targetFormat]
val w = parse(options.width, 80f, 5f, 220f)
val h = parse(options.height, 60f, 5f, 220f)
val baseValue = parse(options.base, 1.2f, 0.4f, 10f)
val reliefValue = parse(options.relief, 4f, 0.8f, 15f)
var b = baseValue
var z = reliefValue
val frameBase = parse(options.frameBase, 5f, 2f, 20f)
val standDepth = parse(options.footDepth, 24f, 8f, 60f)
val openingDiameter = parse(options.openingDiameter, 18f, 0f, 60f)
val selectedMode = if (targetFormat != 0 && options.mode > 1) 2 else options.mode.coerceIn(0, 10)
val detail = intArrayOf(64, 96, 128, 192)[options.resolution.coerceIn(0, 3)]
val modelDiameter = parse(options.diameter, 90f, 20f, 220f)
val modelTopDiameter = parse(options.topDiameter, modelDiameter, 10f, 220f)
val imageAngle = Math.max(24, options.wrapAngle)
val waves = options.waveCount
val waveAmplitude = parse(options.waveDepth, 3f, 0f, 12f)
val bottomClosed = options.closeBottom
val topClosed = options.closeTop
val makeBase = options.createLightBase
val baseOuter = modelDiameter
val baseInner = Math.max(12f, Math.min(40f, modelDiameter * 0.55f))
val accessoryBaseThickness = parse(options.lightBaseThickness, 7.5f, 2f, 20f)
val cableDiameter = parse(options.cableDiameter, 6f, 0f, 30f)
val inverted = options.invert
val brightnessAdjustment = (options.brightness - 100) / 100f
val contrastAdjustment = options.contrast / 100f
val selectedToneMode = options.toneMode
if (selectedMode >= 2)
{
b = parse(options.base, 0.8f, 0.4f, 8f)
z = parse(options.relief, 3f, b + 0.2f, 15f)
}
val baseThickness = b
val maxRelief = z
if ((selectedMode >= 2) && z <= b)
{
Toast.makeText(activity, R.string.LithophaneMaxMustExceedMin, Toast.LENGTH_LONG).show()
return
}
val arcRadius = parse(options.curveRadius, 55f, 25f, 180f)
if (selectedMode == 5 && (!"stl".equals(extension) || arcRadius < w / (Math.PI).toFloat()))
{
Toast.makeText(activity, R.string.LithophaneCurveRequiresStl, Toast.LENGTH_LONG).show()
return
}
if (selectedMode == 3 && images.size != 4)
{
Toast.makeText(activity, R.string.LithophaneCubeNeedsFourImages, Toast.LENGTH_LONG).show()
return
}
if ((selectedMode == 4 || selectedMode == 5 || selectedMode >= 6) && !"stl".equals(extension))
{
Toast.makeText(activity, R.string.LithophaneFrameRequiresStl, Toast.LENGTH_LONG).show()
return
}
if (selectedMode == 4 && (frameBase <= b || standDepth < openingDiameter + 3f))
{
Toast.makeText(activity, R.string.LithophaneFrameDimensionsInvalid, Toast.LENGTH_LONG).show()
return
}
if (images.isEmpty())
{
Toast.makeText(activity, R.string.ConverterPickImage, Toast.LENGTH_LONG).show()
return
}
val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
IOUtils.IO_POOL.execute({ val stem = if (selectedMode == 3) "lithophane-cube-kit" else if (selectedMode == 4) "lithophane-frame" else if (selectedMode == 5) "lithophane-curved" else if (selectedMode == 2) "lithophane" else "converted-image"
val output = File(activity.getCacheDir(), stem + "-" + System.currentTimeMillis() + "." + extension)
val accessory = if (makeBase && selectedMode == 10)
File(activity.getCacheDir(), "lithophane-light-base-" + System.currentTimeMillis() + ".stl")
else
null
progress.setOutputFile(output)
progress.setStage(10, "Bild wird vorbereitet â€¦")
try
{
if ("stl".equals(extension) && selectedMode == 3)
{
val cols = gridColumns(detail, w, h)
val rows = gridRows(detail, w, h)
val panels = ArrayList<FloatArray>(4)
for (i in 0..3)
{
val bitmap = decodeBitmap(activity.getContentResolver(), images.get(i))
try
{
panels.add(createTopZ(bitmap, cols, rows, baseThickness, maxRelief, 3, inverted, brightnessAdjustment, contrastAdjustment, selectedToneMode))
}
finally
{
bitmap.recycle()
}
}
LithophaneStlWriter.writeCubeKit(output, panels, cols, rows, w, h, baseThickness, 3f)
}
else if ("stl".equals(extension) && selectedMode >= 6)
{
val bitmap = decodeBitmap(activity.getContentResolver(), images.get(0))
try
{
val around = Math.max(24, detail * 2)
val vertical = detail
var roundHeight = if (selectedMode == 6) modelDiameter else h
if (selectedMode == 6) roundHeight = Math.max(20f, Math.min(modelDiameter, modelDiameter * imageAngle / 180f))
val roundDiameter = modelDiameter
val roundTopDiameter = if (selectedMode == 7) modelTopDiameter else modelDiameter
val wrap = Bitmap.createBitmap(Math.max(8, around), vertical, Bitmap.Config.ARGB_8888)
try
{
val canvas = android.graphics.Canvas(wrap)
val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, wrap.getWidth(), wrap.getHeight()), paint)
val rotationalShape = selectedMode - 6
LithophaneStlWriter.writeRotationalLithophane(output,
createTopZ(wrap, wrap.getWidth(), wrap.getHeight(), baseThickness, maxRelief,
2, inverted, brightnessAdjustment, contrastAdjustment, selectedToneMode),
wrap.getWidth(), wrap.getHeight(), roundDiameter, roundHeight,
rotationalShape, roundTopDiameter, if (waves > 0) Math.min(waveAmplitude, modelDiameter * 0.12f) else 0f,
waves, bottomClosed, topClosed)
if (accessory != null)
{
LithophaneStlWriter.writeRoundLightBase(accessory, baseOuter, baseInner,
accessoryBaseThickness, cableDiameter, Math.max(48, detail))
}
}
finally
{
wrap.recycle()
}
}
finally
{
bitmap.recycle()
}
}
else if ("stl".equals(extension) && selectedMode == 5)
{
val bitmap = decodeBitmap(activity.getContentResolver(), images.get(0))
try
{
val cols = gridColumns(detail, w, h)
val rows = gridRows(detail, w, h)
val topZ = createTopZ(bitmap, cols, rows, baseThickness, maxRelief, 2, inverted, brightnessAdjustment, contrastAdjustment, selectedToneMode)
LithophaneStlWriter.writeCurvedPanel(output, topZ, cols, rows, w, h, arcRadius)
}
finally
{
bitmap.recycle()
}
}
else
{
val image = images.get(0)
val bitmap = decodeBitmap(activity.getContentResolver(), image)
try
{
if ("stl".equals(extension) && selectedMode == 4)
{
val cols = gridColumns(detail, w, h)
val rows = gridRows(detail, w, h)
val topZ = createTopZ(bitmap, cols, rows, baseThickness, maxRelief, 2, inverted, brightnessAdjustment, contrastAdjustment, selectedToneMode)
LithophaneStlWriter.writeFramedPanel(output, topZ, cols, rows, w, h,
frameBase, standDepth, openingDiameter, Math.max(1f, Math.min(15f, w * 0.05f)))
}
else if ("stl".equals(extension))
writeStl(bitmap, output, w, h, baseThickness, maxRelief, selectedMode, inverted, detail, brightnessAdjustment, contrastAdjustment, selectedToneMode)
else if ("svg".equals(extension))
writeSvg(bitmap, output, selectedMode)
else
writeRaster(bitmap, output, extension)
}
finally
{
bitmap.recycle()
}
}
progress.setStage(92, "Ergebnis wird geprÃ¼ft â€¦")
validateGeneratedOutput(output, extension)
progress.complete({ if (accessory != null)
showGeneratedPair(activity, output, accessory, bed)
else if (saveAfterGeneration)
requestSave(activity, output, extension, bed)
else
showGeneratedOutput(activity, output, extension, bed) })
}
catch (e:Exception) {
progress.fail()
if (output.exists()) output.delete()
if (accessory != null && accessory.exists()) accessory.delete()
showError(activity, e)
}
})
}
private fun updateFrameOptions(mode:Int, targetVisibility:Int, baseLabel:TextView, base:EditText,
footLabel:TextView, foot:EditText, openingLabel:TextView, opening:EditText) {
val visibility = if (mode == 4 && targetVisibility == android.view.View.VISIBLE)
android.view.View.VISIBLE
else
android.view.View.GONE
baseLabel.setVisibility(visibility)
base.setVisibility(visibility)
footLabel.setVisibility(visibility)
foot.setVisibility(visibility)
openingLabel.setVisibility(visibility)
opening.setVisibility(visibility)
}
private fun updateCurvedOptions(mode:Int, targetPosition:Int, radiusLabel:TextView, radius:EditText) {
val visibility = if (mode == 5 && targetPosition == 0) View.VISIBLE else View.GONE
radiusLabel.setVisibility(visibility)
radius.setVisibility(visibility)
}
private fun updateRoundOptions(mode:Int, targetPosition:Int, vararg roundControls:View) {
val visible = targetPosition == 0 && mode >= 6 && mode <= 10
for (control in roundControls) if (control != null) control.setVisibility(if (visible) View.VISIBLE else View.GONE)
val tapered = mode == 7 || mode == 8 || mode == 9 || mode == 10
setControls(roundControls, visible && tapered, 3, 4)
setControls(roundControls, visible && (mode == 9 || mode == 10), 7, 8, 9, 10)
setControls(roundControls, visible && mode == 10, 13, 14, 15, 16, 17)
setControls(roundControls, false, 18, 19)
}
private fun setControls(controls:Array<out View>, visible:Boolean, vararg indices:Int) {
for (index in indices)
    if (index < controls.size && controls[index] != null)
controls[index].setVisibility(if (visible) View.VISIBLE else View.GONE)
}
internal fun showLithophaneImageEditor(
    activity: Activity,
    image: Uri,
    bed: BedFragment,
    savedSettings: IntArray,
    savedInvert: BooleanArray,
    onClosed: () -> Unit = {},
) {
if (bed.canShowWorkspaceComposeDialog()) {
    val resumed = AtomicBoolean(false)
    val reopenParent = { if (resumed.compareAndSet(false, true)) onClosed() }
    bed.showWorkspaceComposeDialog(onExternalDismiss = reopenParent) { dismiss ->
        LithophaneToneEditorDialog(
            activity = activity,
            image = image,
            initialToneMode = savedSettings[0],
            initialBrightness = savedSettings[1],
            initialContrast = savedSettings[2],
            initialInvert = savedInvert[0],
            onDismiss = { dismiss(); reopenParent() },
            onApply = { toneMode, brightness, contrast, invert ->
                savedSettings[0] = toneMode
                savedSettings[1] = brightness
                savedSettings[2] = contrast
                savedInvert[0] = invert
                dismiss()
                reopenParent()
            },
        )
    }
    return
}
showLithophaneImageEditorLegacy(activity, image, savedSettings, savedInvert, onClosed)
}

private fun showLithophaneImageEditorLegacy(activity:Activity, image:Uri, savedSettings:IntArray,
savedInvert:BooleanArray, onClosed: () -> Unit) {
val editor = LinearLayout(activity)
editor.setOrientation(LinearLayout.VERTICAL)
editor.setPadding(ViewUtils.dp(20), ViewUtils.dp(8), ViewUtils.dp(20), ViewUtils.dp(8))
val preview = ImageView(activity)
preview.setScaleType(ImageView.ScaleType.FIT_CENTER)
preview.setAdjustViewBounds(true)
preview.setBackgroundColor(Color.rgb(38, 40, 44))
preview.setContentDescription(activity.getString(R.string.LithophanePreviewCaption))
val maxPreviewHeight = Math.min(ViewUtils.dp(420), activity.getResources().getDisplayMetrics().heightPixels * 42 / 100)
editor.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
Math.max(ViewUtils.dp(220), maxPreviewHeight)))
val previewHint = TextView(activity)
previewHint.setText(R.string.LithophanePreviewCaption)
editor.addView(previewHint)
val toneLabel = TextView(activity)
toneLabel.setText(R.string.LithophaneGrayscaleMode)
editor.addView(toneLabel)
val toneMode = Spinner(activity)
toneMode.setAdapter(ArrayAdapter.createFromResource(activity, R.array.LithophaneGrayscaleModes,
android.R.layout.simple_spinner_dropdown_item))
toneMode.setSelection(Math.max(0, Math.min(savedSettings[0], 5)))
editor.addView(toneMode)
val brightnessLabel = TextView(activity)
editor.addView(brightnessLabel)
val brightness = android.widget.SeekBar(activity)
brightness.setMax(200)
brightness.setProgress(Math.max(0, Math.min(savedSettings[1], 200)))
editor.addView(brightness)
val contrastLabel = TextView(activity)
editor.addView(contrastLabel)
val contrast = android.widget.SeekBar(activity)
contrast.setMax(200)
contrast.setProgress(Math.max(0, Math.min(savedSettings[2], 200)))
editor.addView(contrast)
val help = TextView(activity)
help.setText(R.string.LithophaneToneHelp)
editor.addView(help)
val invert = CheckBox(activity)
invert.setText(R.string.LithophaneInvert)
invert.setChecked(savedInvert[0])
editor.addView(invert)
val scroll = ScrollView(activity)
scroll.setFillViewport(true)
scroll.addView(editor, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.WRAP_CONTENT))
val maxDialogHeight = activity.getResources().getDisplayMetrics().heightPixels * 86 / 100
val dialog = BeamAlertDialogBuilder(activity)
.setTitle(R.string.LithophaneImageEditor)
.setView(scroll)
.setNegativeButton(android.R.string.cancel, null)
.setPositiveButton(R.string.ConverterApply, null)
.create()
val updateLabels = { brightnessLabel.setText(activity.getString(R.string.LithophaneBrightnessValue, brightness.getProgress() - 100))
contrastLabel.setText(activity.getString(R.string.LithophaneContrastValue, contrast.getProgress())) }
var cancelPreview: (() -> Unit)? = null
val updatePreview = {
cancelPreview?.invoke()
cancelPreview = requestLithophanePreview(
    activity, image, invert.isChecked(), toneMode.getSelectedItemPosition(),
    (brightness.getProgress() - 100) / 100f, contrast.getProgress() / 100f,
) { result ->
    if (result != null && preview.isAttachedToWindow) {
        val old = (preview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        if (old != null && old !== result && !old.isRecycled) old.recycle()
        preview.setImageBitmap(result)
    } else result?.recycle()
}
}
val sliderListener = object:android.widget.SeekBar.OnSeekBarChangeListener {
override fun onProgressChanged(seekBar:android.widget.SeekBar, progress:Int, fromUser:Boolean) {
updateLabels()
updatePreview()
}
override fun onStartTrackingTouch(seekBar:android.widget.SeekBar) {}
override fun onStopTrackingTouch(seekBar:android.widget.SeekBar) {}
}
brightness.setOnSeekBarChangeListener(sliderListener)
contrast.setOnSeekBarChangeListener(sliderListener)
toneMode.setOnItemSelectedListener(object:android.widget.AdapterView.OnItemSelectedListener {
override fun onItemSelected(parent:android.widget.AdapterView<*>, view:View, position:Int, id:Long) {
updatePreview()
}
override fun onNothingSelected(parent:android.widget.AdapterView<*>) {}
})
invert.setOnCheckedChangeListener({ button, checked-> updatePreview() })
dialog.show()
dialog.getWindow()!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, maxDialogHeight)
dialog.setOnDismissListener {
cancelPreview?.invoke()
val old = (preview.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
if (old != null && !old.isRecycled) old.recycle()
onClosed()
}
dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener({ v->
savedSettings[0] = toneMode.getSelectedItemPosition()
savedSettings[1] = brightness.getProgress()
savedSettings[2] = contrast.getProgress()
savedInvert[0] = invert.isChecked()
dialog.dismiss() })
updateLabels()
updatePreview()
}
internal fun requestLithophanePreview(
    activity: Activity,
    uri: Uri,
    invert: Boolean,
    toneMode: Int,
    brightness: Float,
    contrast: Float,
    onResult: (Bitmap?) -> Unit,
): () -> Unit {
val active = AtomicBoolean(true)
IOUtils.IO_POOL.execute({ var rendered:Bitmap? = null
try
{
val source = decodeBitmap(activity.getContentResolver(), uri, 1024)
try
{
rendered = renderLithophanePreview(source, invert, 2, toneMode, brightness, contrast)
}
finally
{
source.recycle()
}
}
catch (ignored:Exception) {}
val result = rendered
ViewUtils.postOnMainThread({
if (!active.get() || activity.isFinishing || activity.isDestroyed)
{
result?.recycle()
return@postOnMainThread
}
onResult(result)
}) })
return { active.set(false) }
}
internal fun renderLithophanePreview(source:Bitmap, invert:Boolean, mode:Int,
toneMode:Int, brightness:Float, contrast:Float):Bitmap {
val width = source.getWidth()
val height = source.getHeight()
val pixels = IntArray(width * height)
val gray = FloatArray(pixels.size)
for (y in 0 until height)
    for (x in 0 until width)
{
val color = source.getPixel(x, y)
val index = y * width + x
val alpha = ((color.ushr(24)) and 255) / 255f
val red = Color.red(color) / 255f
val green = Color.green(color) / 255f
val blue = Color.blue(color) / 255f
var value = LithophaneToneMapper.gray(red, green, blue, alpha,
brightness, contrast, toneMode, invert)
if (mode == 1) value = if (value < 0.5f) 0.08f else 0.96f
gray[index] = value
}
for (y in 0 until height)
    for (x in 0 until width)
{
val index = y * width + x
val left = gray[y * width + Math.max(0, x - 1)]
val right = gray[y * width + Math.min(width - 1, x + 1)]
val up = gray[Math.max(0, y - 1) * width + x]
val down = gray[Math.min(height - 1, y + 1) * width + x]
val shade = Math.max(0.62f, Math.min(1.18f, 0.9f + (right - left) * 0.22f + (down - up) * 0.16f))
val channel = Math.max(0, Math.min(255, Math.round(gray[index] * shade * 255f)))
pixels[index] = Color.rgb(channel, channel, channel)
}
val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
result.setPixels(pixels, 0, width, 0, 0, width, height)
return result
}
private fun updateGeneratorOptions(mode:Int, targetVisibility:Int, width:EditText, height:EditText,
baseLabel:TextView, base:EditText, reliefLabel:TextView,
relief:EditText, resolutionLabel:TextView, resolution:Spinner) {
val stl = targetVisibility == android.view.View.VISIBLE
val visibility = if (stl) android.view.View.VISIBLE else android.view.View.GONE
width.setVisibility(visibility)
height.setVisibility(visibility)
baseLabel.setVisibility(visibility)
base.setVisibility(visibility)
reliefLabel.setVisibility(visibility)
relief.setVisibility(visibility)
resolutionLabel.setVisibility(visibility)
resolution.setVisibility(visibility)
baseLabel.setText(if (mode == 2 || mode == 3 || mode == 4 || mode == 5) R.string.LithophaneMinThickness else R.string.ImageToStlBase)
reliefLabel.setText(if (mode == 2 || mode == 3 || mode == 4 || mode == 5) R.string.LithophaneMaxThickness else R.string.ImageToStlReliefHeight)
}
@JvmStatic
fun saveGeneratedStl(activity:Activity, output:File, bed:BedFragment) {
try
{
validateGeneratedOutput(output, "stl")
showGeneratedOutput(activity, output, "stl", bed)
}
catch (error:Exception) {
if (output.exists()) output.delete()
showError(activity, error)
}
}
private fun showGeneratedPair(activity:Activity, model:File, accessory:File, bed:BedFragment) {
if (bed.canShowWorkspaceComposeDialog()) {
    bed.showWorkspaceComposeDialog { dismiss ->
        GeneratedPairOutputDialog(
            onDismiss = dismiss,
            onLoadModelOnly = {
                dismiss()
                try { bed.loadModel(model) } catch (e:Exception) { showError(activity, e) }
                accessory.delete()
            },
            onLoadBoth = {
                dismiss()
                try {
                    bed.loadModel(model)
                    bed.loadModel(accessory)
                } catch (e:Exception) { showError(activity, e) }
            },
            onSaveModel = {
                dismiss()
                accessory.delete()
                requestSave(activity, model, "stl", bed)
            },
            onSaveAccessory = {
                dismiss()
                model.delete()
                requestSave(activity, accessory, "stl", bed)
            },
            onSaveBoth = {
                dismiss()
                requestSave(activity, accessory, "stl", bed)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    { requestSave(activity, model, "stl", bed) }, 1200L,
                )
            },
        )
    }
    return
}
BeamAlertDialogBuilder(activity).setTitle(R.string.GeneratedOutputTitle)
.setMessage(R.string.LithophanePairQuestion)
.setNegativeButton(R.string.LithophaneGenerate, { d, w->
if (bed != null)
{
try
{
bed.loadModel(model)
}
catch (e:Exception) {
showError(activity, e)
}
}
accessory.delete() })
.setPositiveButton(R.string.ConverterLoad, { d, w-> if (bed != null)
{
try
{
bed.loadModel(model)
bed.loadModel(accessory)
}
catch (e:Exception) {
showError(activity, e)
}
} })
.setNeutralButton(R.string.ConverterOnlySave, { d, w-> BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterSavedTitle)
.setMessage("Welche STL-Datei mÃ¶chtest du speichern?")
.setItems(intArrayOf(
R.string.LithophaneSaveModelOnly, R.string.LithophaneSaveAccessoryOnly, R.string.LithophaneSaveBoth,
).map { activity.getText(it) }.toTypedArray(),
{ dialog, which-> if (which == 0)
{
accessory.delete()
requestSave(activity, model, "stl", bed)
}
else if (which == 1)
{
model.delete()
requestSave(activity, accessory, "stl", bed)
}
else
{
requestSave(activity, accessory, "stl", bed)
val handler = android.os.Handler(android.os.Looper.getMainLooper())
handler.postDelayed({ requestSave(activity, model, "stl", bed) }, 1200L)
} }).show() }).show()
}
/** Shares the generated-file save/load flow with native generators that produce other formats. */
 @JvmStatic
 fun saveGeneratedFile(activity:Activity, output:File, extension:String, bed:BedFragment) {
try
{
if ("3mf".equals(extension))
{
java.util.zip.ZipFile(output).use({ zip-> if (zip.getEntry("[Content_Types].xml") == null || zip.getEntry("3D/3dmodel.model") == null)
throw IOException("Die erzeugte 3MF-Datei ist unvollstÃ¤ndig.") })
}
else if (("stl".equals(extension) || "svg".equals(extension) || "png".equals(extension)
|| "jpg".equals(extension) || "jpeg".equals(extension) || "webp".equals(extension)))
{
validateGeneratedOutput(output, extension)
}
else if (output == null || !output.isFile() || output.length() == 0L)
{
throw IOException("Die erzeugte Datei ist leer oder fehlt.")
}
showGeneratedOutput(activity, output, extension, bed)
}
catch (error:Exception) {
if (output != null && output.exists()) output.delete()
showError(activity, error)
}
}
internal fun showGeneratedOutput(activity:Activity, output:File, extension:String, bed:BedFragment) {
val loadable = ("stl".equals(extension) || "svg".equals(extension) || "3mf".equals(extension)) && bed != null
if (!loadable)
{
if ("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension) || "webp".equals(extension))
{
showRasterOutputPreview(activity, output, extension, bed)
return
}
requestSave(activity, output, extension, bed)
return
}
if (bed.canShowWorkspaceComposeDialog()) {
    bed.showWorkspaceComposeDialog { dismiss ->
        GeneratedModelOutputDialog(
            message = output.name + "\n\n" + activity.getString(R.string.GeneratedOutputQuestion),
            onDismiss = dismiss,
            onDiscard = { dismiss(); output.delete() },
            onSave = { dismiss(); requestSave(activity, output, extension, bed) },
            onUse = {
                dismiss()
                try {
                    bed.loadModel(output, false, 1, object:BedFragment.ModelLoadCallback {
                        override fun onLoaded(model:ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject:Int, addedObjects:Int) {
                            output.delete()
                        }
                    })
                } catch (error:Exception) { showError(activity, error) }
            },
        )
    }
    return
}
BeamAlertDialogBuilder(activity).setTitle(R.string.GeneratedOutputTitle)
.setMessage(output.getName() + "\n\n" + activity.getString(R.string.GeneratedOutputQuestion))
.setNegativeButton(R.string.GeneratedOutputCancel, { d, w-> output.delete() })
.setNeutralButton(R.string.GeneratedOutputSave, { d, w-> requestSave(activity, output, extension, bed) })
.setPositiveButton(R.string.GeneratedOutputUse, { d, w-> try
{
bed.loadModel(output, false, 1, object:BedFragment.ModelLoadCallback {
override fun onLoaded(model:ru.ytkab0bp.slicebeam.slic3r.Model,
firstNewObject:Int, addedObjects:Int) {
output.delete()
}
})
}
catch (error:Exception) {
showError(activity, error)
}
}).show()
}
private fun showRasterOutputPreview(activity:Activity, output:File, extension:String, bed:BedFragment) {
val bitmap = BitmapFactory.decodeFile(output.getAbsolutePath())
if (bitmap == null)
{
requestSave(activity, output, extension, bed)
return
}
if (bed.canShowWorkspaceComposeDialog()) {
    bed.showWorkspaceComposeDialog { dismiss ->
        GeneratedRasterPreviewDialog(
            bitmap = bitmap,
            onDismiss = dismiss,
            onDiscard = { dismiss(); output.delete() },
            onSave = { dismiss(); requestSave(activity, output, extension, bed) },
        )
    }
    return
}
val preview = ImageView(activity)
preview.setImageBitmap(bitmap)
preview.setScaleType(ImageView.ScaleType.FIT_CENTER)
preview.setBackgroundColor(Color.rgb(38, 40, 44))
preview.setContentDescription(activity.getString(R.string.ConverterOutputPreview))
preview.setAdjustViewBounds(true)
preview.setMaxHeight(ViewUtils.dp(300))
BeamAlertDialogBuilder(activity)
.setTitle(R.string.ConverterOutputPreview)
.setView(preview)
.setNegativeButton(R.string.GeneratedOutputCancel, { d, w->
bitmap.recycle()
output.delete() })
.setPositiveButton(R.string.ConverterSaveNow, { d, w->
bitmap.recycle()
requestSave(activity, output, extension, bed) })
.show()
}
@Throws(IOException::class)
private fun validateGeneratedOutput(file:File, extension:String) {
if (file == null || !file.isFile() || file.length() < 16)
    throw IOException("Die erzeugte Datei ist leer oder unvollstÃ¤ndig.")
if ("stl".equals(extension))
{
RandomAccessFile(file, "r").use({ input->
if (input.length() < 84) throw IOException("Die STL-Datei enthÃ¤lt keine gÃ¼ltigen Dreiecke.")
input.seek(80)
val facets = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
if (facets < 4 || facets > (input.length() - 84) / 50 || 84L + facets * 50L != input.length())
    throw IOException("Die STL-Datei ist beschÃ¤digt oder enthÃ¤lt kein vollstÃ¤ndiges Modell.")
var minX = Float.POSITIVE_INFINITY
var minY = Float.POSITIVE_INFINITY
var minZ = Float.POSITIVE_INFINITY
var maxX = Float.NEGATIVE_INFINITY
var maxY = Float.NEGATIVE_INFINITY
var maxZ = Float.NEGATIVE_INFINITY
for (i in 0 until facets)
{
input.skipBytes(12)
for (v in 0..2)
{
val x = readLittleFloat(input)
val y = readLittleFloat(input)
val z = readLittleFloat(input)
if (!java.lang.Float.isFinite(x) || !java.lang.Float.isFinite(y) || !java.lang.Float.isFinite(z))
    throw IOException("Die STL-Geometrie enthÃ¤lt ungÃ¼ltige Koordinaten.")
minX = Math.min(minX, x)
maxX = Math.max(maxX, x)
minY = Math.min(minY, y)
maxY = Math.max(maxY, y)
minZ = Math.min(minZ, z)
maxZ = Math.max(maxZ, z)
}
input.skipBytes(2)
}
if (maxX - minX < 0.1f || maxY - minY < 0.1f || maxZ - minZ < 0.1f)
throw IOException("Das Modell hat keine sinnvollen Abmessungen.") })
}
else if ("svg".equals(extension))
{
val bytes = ByteArrayOutputStream()
java.io.FileInputStream(file).use({ input->
val buffer = ByteArray(4096)
var read:Int
var total = 0
while (true)
{
val read = input.read(buffer)
if (read == -1) break
if (!(total < 1024 * 1024)) break
bytes.write(buffer, 0, read)
total += read
}
})
val svg = String(bytes.toByteArray(), StandardCharsets.UTF_8)
if (!svg.contains("<svg") || !(svg.contains("<path") || svg.contains("<polygon") || svg.contains("<rect")))
    throw IOException("Die SVG-Datei enthÃ¤lt keine verwertbare Kontur.")
}
else
{
val bounds = BitmapFactory.Options()
bounds.inJustDecodeBounds = true
BitmapFactory.decodeFile(file.getAbsolutePath(), bounds)
if (bounds.outWidth < 2 || bounds.outHeight < 2)
    throw IOException("Die erzeugte Bilddatei ist ungÃ¼ltig oder leer.")
}
}
@Throws(IOException::class)
private fun readLittleFloat(input:RandomAccessFile):Float {
return java.lang.Float.intBitsToFloat(Integer.reverseBytes(input.readInt()))
}
internal fun requestSave(activity:Activity, output:File, extension:String, bed:BedFragment) {
pendingOutput = output
pendingExtension = extension
pendingBed = WeakReference(bed)
if (ConverterStorage.hasRoot())
{
finishSaveToRoot(activity)
return
}
try
{
activity.launchAppActivityResult(
    ConverterStorage.createFolderPickerIntent(),
    MainActivity.REQUEST_CODE_CONVERTER_ROOT_SAVE,
)
}
catch (error:Exception) {
requestSavePicker(activity, output, extension, bed)
}
}
@JvmStatic
fun onConverterRootCancelled(activity:Activity) {
val output = pendingOutput
val extension = pendingExtension
val bed = pendingBed.get()
if (output == null || extension == null) return
requestSavePicker(activity, output, extension, bed)
}
private fun requestSavePicker(activity:Activity, output:File, extension:String, bed:BedFragment?) {
pendingOutput = output
pendingExtension = extension
pendingBed = WeakReference(bed)
val save = Intent(Intent.ACTION_CREATE_DOCUMENT)
save.addCategory(Intent.CATEGORY_OPENABLE)
save.setType(if ("stl".equals(extension))
"model/stl"
else if ("3mf".equals(extension))
"model/3mf"
else if ("svg".equals(extension))
"image/svg+xml"
else if ("jpg".equals(extension) || "jpeg".equals(extension))
"image/jpeg"
else if ("png".equals(extension) || "webp".equals(extension))
"image/" + extension
else
"application/octet-stream")
save.putExtra(Intent.EXTRA_TITLE, output.getName())
activity.launchAppActivityResult(save, MainActivity.REQUEST_CODE_SAVE_CONVERTED_FILE)
}
@JvmStatic
fun finishSaveToRoot(activity:Activity) {
val output = pendingOutput
val extension = pendingExtension
val bed = pendingBed.get()
val root = ConverterStorage.getRoot()
if (output == null || extension == null || root == null) return
pendingOutput = null
pendingExtension = null
pendingBed.clear()
IOUtils.IO_POOL.execute({ try
{
val folderName = ConverterStorage.saveGeneratedFile(activity, root, output)
ViewUtils.postOnMainThread({ if (activity.isFinishing()) {
output.delete()
return@postOnMainThread
}
Toast.makeText(activity, activity.getString(R.string.ConverterSavedInFolder, folderName), Toast.LENGTH_LONG).show()
showSavedOutputQuestion(activity, output, extension!!, bed) })
}
catch (error:Exception) {
if (ConverterStorage.isPermissionFailure(error)) {
    android.util.Log.w("ConverterStorage", "Saved folder permission is no longer valid; falling back to Android's save picker", error)
    ConverterStorage.clearRoot(activity)
    ViewUtils.postOnMainThread({
        if (activity.isFinishing()) {
            output.delete()
            return@postOnMainThread
        }
        requestSavePicker(activity, output, extension!!, bed)
    })
    return@execute
}
showError(activity, if (error is IOException) error as IOException else IOException(error))
}
})
}
@JvmStatic
fun finishSave(activity:Activity, destination:Uri?) {
val output = pendingOutput
val extension = pendingExtension
val bed = pendingBed.get()
pendingOutput = null
pendingExtension = null
pendingBed.clear()
if (output == null || !output.isFile()) return
if (destination == null) {
output.delete()
return
}
try
{
java.io.FileInputStream(output).use({ `in`-> activity.getContentResolver().openOutputStream(destination, "wt").use({ out->
if (out == null) throw IOException("Die Zieldatei konnte nicht geÃ¶ffnet werden.")
val buffer = ByteArray(16 * 1024)
var count:Int
while (true) { val count = `in`.read(buffer); if (count == -1) break; out.write(buffer, 0, count) } }) })
}
catch (e:Exception) {
showError(activity, e)
return
}
showSavedOutputQuestion(activity, output, extension!!, bed)
}
internal fun showSavedOutputQuestion(activity:Activity, output:File, extension:String, bed:BedFragment?) {
if (("stl".equals(extension) || "svg".equals(extension) || "3mf".equals(extension)) && bed != null)
{
if (bed.canShowWorkspaceComposeDialog()) {
    bed.showWorkspaceComposeDialog { dismiss ->
        GeneratedSavedOutputDialog(
            onDismiss = dismiss,
            onKeepSaved = { dismiss(); output.delete() },
            onLoad = {
                dismiss()
                try {
                    bed.loadModel(output)
                } catch (e:Exception) {
                    showError(activity, e)
                }
            },
        )
    }
    return
}
BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterSavedTitle)
.setMessage(R.string.ConverterLoadQuestion)
.setNegativeButton(R.string.ConverterOnlySave, { d, w-> output.delete() })
.setPositiveButton(R.string.ConverterLoad, { d, w-> try
{
bed.loadModel(output)
}
catch (e:Exception) {
showError(activity, e)
}
}).show()
}
else
{
output.delete()
}
}
@Throws(IOException::class)
private fun writeRaster(bitmap:Bitmap, file:File, extension:String) {
val format = if ("png".equals(extension))
Bitmap.CompressFormat.PNG
else if ("jpg".equals(extension)) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.WEBP
BufferedOutputStream(FileOutputStream(file)).use({ out-> if (!bitmap.compress(format, 95, out)) throw IOException("Das Bild konnte nicht gespeichert werden.") })
}
internal fun decodePreviewBitmap(activity: Activity, uri: Uri): Bitmap =
    decodeBitmap(activity.getContentResolver(), uri, 512)
@Throws(IOException::class)
private fun decodeBitmap(resolver:ContentResolver, uri:Uri):Bitmap {
return decodeBitmap(resolver, uri, MAX_SIDE)
}
@Throws(IOException::class)
internal fun decodeBitmap(resolver:ContentResolver, uri:Uri, maxSide:Int):Bitmap {
val bounds = BitmapFactory.Options()
bounds.inJustDecodeBounds = true
resolver.openInputStream(uri).use({ `in`->
if (`in` == null) throw IOException("Das Bild kann nicht geÃ¶ffnet werden.")
BitmapFactory.decodeStream(`in`, null, bounds) })
if (bounds.outWidth < 2 || bounds.outHeight < 2) throw IOException("Das Bildformat wird nicht unterstÃ¼tzt oder die Datei ist beschÃ¤digt.")
var sample = 1
while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2
val opts = BitmapFactory.Options()
opts.inSampleSize = sample
opts.inPreferredConfig = Bitmap.Config.ARGB_8888
resolver.openInputStream(uri).use({ `in`->
val bitmap = if (`in` == null) null else BitmapFactory.decodeStream(`in`, null, opts)
if (bitmap == null) throw IOException("Das Bild konnte nicht decodiert werden.")
return bitmap })
}
@Throws(IOException::class)
private fun writeSvg(bitmap:Bitmap, file:File, mode:Int) {
val sample = Math.max(1, (Math.ceil(Math.max(bitmap.getWidth(), bitmap.getHeight()) / MAX_VECTOR_SIDE.toDouble())).toInt())
val width = Math.max(2, (bitmap.getWidth() + sample - 1) / sample)
val height = Math.max(2, (bitmap.getHeight() + sample - 1) / sample)
val mask = Array<BooleanArray>(height, {BooleanArray(width)})
for (y in 0 until height)
    for (x in 0 until width)
{
mask[y][x] = isFilled(bitmap.getPixel(Math.min(bitmap.getWidth() - 1, x * sample),
Math.min(bitmap.getHeight() - 1, y * sample)), mode)
}
removeTinyComponents(mask, width, height)
val edges = ArrayList<SvgEdge>()
val outgoing = HashMap<Long, MutableList<Int>>()
for (y in 0 until height)
    for (x in 0 until width)
if (mask[y][x])
{
if (!filled(mask, x, y - 1)) addEdge(edges, outgoing, x, y, x + 1, y, 0)
if (!filled(mask, x + 1, y)) addEdge(edges, outgoing, x + 1, y, x + 1, y + 1, 1)
if (!filled(mask, x, y + 1)) addEdge(edges, outgoing, x + 1, y + 1, x, y + 1, 2)
if (!filled(mask, x - 1, y)) addEdge(edges, outgoing, x, y + 1, x, y, 3)
}
val contours = ArrayList<List<SvgPoint>>()
var commandCount = 0
for (first in 0 until edges.size)
{
if (edges.get(first).used) continue
val startEdge = edges.get(first)
val startX = startEdge.x1
val startY = startEdge.y1
var edgeIndex = first
var points: MutableList<SvgPoint> = ArrayList()
var closed = false
for (step in 0..edges.size)
{
val edge = edges.get(edgeIndex)
if (edge.used) break
edge.used = true
points.add(SvgPoint(edge.x1, edge.y1))
if (edge.x2 == startX && edge.y2 == startY) {
closed = true
break
}
val candidates = outgoing.get(pointKey(edge.x2, edge.y2))
edgeIndex = chooseNextEdge(edges, candidates, edge.dir)
if (edgeIndex < 0) break
}
if (!closed) continue
points = simplifyCollinear(points).toMutableList()
points = simplifyClosedContour(points, 0.65).toMutableList()
if (points.size < 3) continue
commandCount += points.size + 1
if (commandCount > 3500) throw IOException("Das Bild enthÃ¤lt zu viele feine Konturen. Verwende ein einfacheres Logo oder eine kontrastreichere Silhouette.")
contours.add(points)
}
BufferedOutputStream(FileOutputStream(file)).use({ out->
val head = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "mm\" height=\"" + height + "mm\" viewBox=\"0 0 " + width + " " + height + "\"><path fill=\"#000\" fill-rule=\"nonzero\" d=\""
out.write(head.toByteArray(StandardCharsets.UTF_8))
val path = StringBuilder(Math.max(128, commandCount * 8))
for (contour in contours)
{
val first = contour.get(0)
path.append('M').append(first.x).append(' ').append(first.y)
for (i in 1 until contour.size) path.append('L').append(contour.get(i).x).append(' ').append(contour.get(i).y)
path.append('Z')
}
out.write(path.toString().toByteArray(StandardCharsets.UTF_8))
out.write("\"/></svg>".toByteArray(StandardCharsets.UTF_8)) })
}
private class SvgPoint internal constructor(internal val x:Int, internal val y:Int)
private class SvgEdge internal constructor(
internal val x1:Int,
internal val y1:Int,
internal val x2:Int,
internal val y2:Int,
internal val dir:Int
) {
internal var used:Boolean = false
}
private fun filled(mask:Array<BooleanArray>, x:Int, y:Int):Boolean {
return y >= 0 && y < mask.size && x >= 0 && x < mask[0].size && mask[y][x]
}
private fun pointKey(x:Int, y:Int):Long {
return (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
}
private fun addEdge(edges:MutableList<SvgEdge>, outgoing:MutableMap<Long, MutableList<Int>>, x1:Int, y1:Int, x2:Int, y2:Int, dir:Int) {
val index = edges.size
edges.add(SvgEdge(x1, y1, x2, y2, dir))
val key = pointKey(x1, y1)
var indexes = outgoing.get(key)
if (indexes == null)
{
indexes = ArrayList(2)
outgoing.put(key, indexes)
}
indexes.add(index)
}
private fun chooseNextEdge(edges:List<SvgEdge>, candidates:List<Int>?, direction:Int):Int {
if (candidates == null) return -1
val turnPriority = intArrayOf(1, 0, 3, 2) // keep the filled pixels on the right, separating corner-touching regions
for (wanted in turnPriority)
    for (index in candidates)
{
val candidate = edges.get(index)
if (!candidate.used && (candidate.dir - direction + 4) % 4 == wanted) return index
}
return -1
}
private fun removeTinyComponents(mask:Array<BooleanArray>, width:Int, height:Int) {
val seen = Array<BooleanArray>(height, {BooleanArray(width)})
val queue = ArrayDeque<Int>()
val component = ArrayList<Int>()
val dx = intArrayOf(1, -1, 0, 0)
val dy = intArrayOf(0, 0, 1, -1)
for (y in 0 until height)
    for (x in 0 until width)
{
if (!mask[y][x] || seen[y][x]) continue
queue.add(y * width + x)
seen[y][x] = true
component.clear()
while (!queue.isEmpty())
{
val value = queue.removeFirst()
val cx = value % width
val cy = value / width
component.add(value)
for (d in 0..3)
{
val nx = cx + dx[d]
val ny = cy + dy[d]
if (nx >= 0 && nx < width && ny >= 0 && ny < height && mask[ny][nx] && !seen[ny][nx])
{
seen[ny][nx] = true
queue.addLast(ny * width + nx)
}
}
}
if (component.size < 3) for (value in component) mask[value / width][value % width] = false
}
}
private fun simplifyCollinear(points:List<SvgPoint>):List<SvgPoint> {
if (points.size < 4) return points
val result = ArrayList<SvgPoint>()
for (i in 0 until points.size)
{
val a = points.get((i + points.size - 1) % points.size)
val b = points.get(i)
val c = points.get((i + 1) % points.size)
if ((b.x - a.x).toLong() * (c.y - b.y) != (b.y - a.y).toLong() * (c.x - b.x)) result.add(b)
}
return result
}
private fun simplifyClosedContour(points:List<SvgPoint>, tolerance:Double):List<SvgPoint> {
if (points.size < 8) return points
val origin = points.get(0)
var split = 1
var farthest = -1.0
for (i in 1 until points.size)
{
val p = points.get(i)
val distance = (p.x - origin.x).toDouble() * (p.x - origin.x) + (p.y - origin.y).toDouble() * (p.y - origin.y)
if (distance > farthest) {
farthest = distance
split = i
}
}
val first = simplifyOpenContour(ArrayList(points.subList(0, split + 1)), tolerance)
val secondInput = ArrayList(points.subList(split, points.size))
secondInput.add(origin)
val second = simplifyOpenContour(secondInput, tolerance)
val result = ArrayList(first)
for (i in 1 until second.size - 1) result.add(second.get(i))
return result
}
private fun simplifyOpenContour(points:List<SvgPoint>, tolerance:Double):List<SvgPoint> {
if (points.size < 3) return points
val keep = BooleanArray(points.size)
keep[points.size - 1] = true
keep[0] = keep[points.size - 1]
val ranges = java.util.ArrayDeque<IntArray>()
ranges.push(intArrayOf(0, points.size - 1))
val toleranceSq = tolerance * tolerance
while (!ranges.isEmpty())
{
val range = ranges.pop()
val a = points.get(range[0])
val b = points.get(range[1])
var maxDistance = toleranceSq
var split = -1
for (i in range[0] + 1 until range[1])
{
val p = points.get(i)
val distance = distanceToSegmentSquared(p, a, b)
if (distance > maxDistance) {
maxDistance = distance
split = i
}
}
if (split != -1) {
keep[split] = true
ranges.push(intArrayOf(range[0], split))
ranges.push(intArrayOf(split, range[1]))
}
}
val result = ArrayList<SvgPoint>()
for (i in 0 until points.size) if (keep[i]) result.add(points.get(i))
return result
}
private fun distanceToSegmentSquared(p:SvgPoint, a:SvgPoint, b:SvgPoint):Double {
val dx = (b.x - a.x).toDouble()
val dy = (b.y - a.y).toDouble()
val length = dx * dx + dy * dy
if (length == 0.0) return (p.x - a.x).toDouble() * (p.x - a.x) + (p.y - a.y).toDouble() * (p.y - a.y)
val t = Math.max(0.0, Math.min(1.0, ((p.x - a.x) * dx + (p.y - a.y) * dy) / length))
val x = a.x + t * dx
val y = a.y + t * dy
return (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y)
}
private fun isFilled(color:Int, mode:Int):Boolean {
val alpha = color.ushr(24)
val gray = (0.2126f * ((color shr 16) and 255) + 0.7152f * ((color shr 8) and 255) + 0.0722f * (color and 255)).toInt()
return alpha >= 64 && (if (mode == 0) gray < 192 else gray < 128)
}
private fun addNumberField(activity:Activity, parent:LinearLayout, label:Int, value:String, addTitle:Boolean = true):EditText {
if (addTitle) {
val title = TextView(activity)
title.setText(label)
parent.addView(title)
}
val edit = EditText(activity)
edit.setSingleLine(true)
edit.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
edit.setText(value)
parent.addView(edit, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
return edit
}
private fun parse(value:String, fallback:Float, min:Float, max:Float):Float {
return ru.ytkab0bp.slicebeam.utils.LithophaneInput.parseClamped(value, fallback, min, max)
}
internal fun showError(activity:Activity, error:Exception) {
ViewUtils.postOnMainThread({ if (activity.isFinishing() || activity.isDestroyed) return@postOnMainThread
val bed = (activity as? MainActivity)?.let {
    runCatching { it.navigationDelegate.getCurrentFragment() as? BedFragment }.getOrNull()
}
if (bed?.canShowWorkspaceComposeDialog() == true) {
    bed.showWorkspaceComposeDialog { dismiss ->
        ConverterErrorDialog(
            message = error.message ?: error.toString(),
            onDismiss = dismiss,
        )
    }
    return@postOnMainThread
}
BeamAlertDialogBuilder(activity).setTitle(R.string.ConverterFailed)
.setMessage(if (error.message == null) error.toString() else error.message)
.setPositiveButton(android.R.string.ok, null).show() })
}
@Throws(IOException::class)
private fun writeStl(bitmap:Bitmap, file:File, width:Float, height:Float, base:Float,
relief:Float, mode:Int, invert:Boolean, resolution:Int,
brightness:Float, contrast:Float, toneMode:Int) {
val cols = gridColumns(resolution, width, height)
val rows = gridRows(resolution, width, height)
val topZ = createTopZ(bitmap, cols, rows, base, relief, mode, invert, brightness, contrast, toneMode)
LithophaneStlWriter.writePanel(file, topZ, cols, rows, width, height)
}
internal fun gridColumns(resolution:Int, width:Float, height:Float):Int {
return if (width >= height) resolution else Math.max(2, Math.round(resolution * width / height))
}
internal fun gridRows(resolution:Int, width:Float, height:Float):Int {
return if (height >= width) resolution else Math.max(2, Math.round(resolution * height / width))
}
private fun createTopZ(source:Bitmap, resolution:Int, base:Float, maxThickness:Float,
mode:Int, invert:Boolean):FloatArray {
val aspect = source.getWidth() / (source.getHeight()).toFloat()
val cols = if (aspect >= 1f) resolution else Math.max(2, Math.round(resolution * aspect))
val rows = if (aspect >= 1f) Math.max(2, Math.round(resolution / aspect)) else resolution
return createTopZ(source, cols, rows, base, maxThickness, mode, invert)
}
private fun createTopZ(source:Bitmap, cols:Int, rows:Int, base:Float,
maxThickness:Float, mode:Int, invert:Boolean,
brightness:Float = 0f, contrast:Float = 1f):FloatArray {
return createTopZ(source, cols, rows, base, maxThickness, mode, invert, brightness, contrast, 0)
}
internal fun createTopZ(source:Bitmap, cols:Int, rows:Int, base:Float,
maxThickness:Float, mode:Int, invert:Boolean,
brightness:Float, contrast:Float, toneMode:Int):FloatArray {
val scaled = Bitmap.createScaledBitmap(source, cols, rows, true)
val topZ = FloatArray(cols * rows)
try
{
for (y in 0 until rows)
    for (x in 0 until cols)
{
val color = scaled.getPixel(x, y)
val alpha = ((color.ushr(24)) and 255) / 255f
val red = ((color shr 16) and 255) / 255f
val green = ((color shr 8) and 255) / 255f
val blue = (color and 255) / 255f
val gray = LithophaneToneMapper.gray(red, green, blue, alpha,
brightness, contrast, toneMode, invert)
topZ[y * cols + x] = LithophaneToneMapper.thickness(gray, base, maxThickness, mode)
}
}
finally
{
if (scaled !== source) scaled.recycle()
}
return topZ
}
}
