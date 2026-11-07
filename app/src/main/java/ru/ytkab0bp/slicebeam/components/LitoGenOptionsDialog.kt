package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** Compose form for LitoGEN; the caller keeps ownership of generation and file actions. */
internal object LitoGenOptionsDialog {
    fun show(
        activity: Activity,
        images: List<Uri>,
        bed: BedFragment,
        onGenerate: (LitoGenOptions, saveAfter: Boolean) -> String?,
    ) {
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.LitoGenTitle, onDismiss = dismiss) {
                LitoGenOptionsContent(activity, images) { options, saveAfter ->
                    val error = onGenerate(options, saveAfter)
                    if (error == null) dismiss()
                    error
                }
            }
        }
    }
}

@Composable
private fun LitoGenOptionsContent(
    activity: Activity,
    images: List<Uri>,
    onGenerate: (LitoGenOptions, saveAfter: Boolean) -> String?,
) {
    var widthText by remember(images) { mutableStateOf("60") }
    var heightText by remember(images) { mutableStateOf("60") }
    var minThicknessText by remember(images) { mutableStateOf("1.2") }
    var maxThicknessText by remember(images) { mutableStateOf("4.0") }
    var brightness by remember(images) { mutableIntStateOf(0) }
    var contrast by remember(images) { mutableIntStateOf(100) }
    var invert by remember(images) { mutableStateOf(false) }
    var detailIndex by remember(images) { mutableIntStateOf(1) }

    val details = stringArrayResource(R.array.LithophaneResolutions)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val wideLayout = configuration.screenWidthDp >= 600
    val dialogHeight = (screenHeight * 0.62f).coerceAtMost(560.dp).coerceAtLeast(200.dp)
    var validationError by remember(images) { mutableStateOf<String?>(null) }
    val previewFilter = remember(brightness, contrast, invert) {
        litoGenPreviewFilter(brightness, contrast, invert)
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(dialogHeight).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (images.size == 5) R.string.LitoGenFiveImageHelp else R.string.LitoGenFourImageHelp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            LitoGenImagePreviews(activity, images, previewFilter)
            Text(
                text = stringResource(R.string.LitoGenPreviewHelp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )

            if (wideLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LitoGenNumberField(R.string.LitoGenWidth, widthText, { widthText = it }, Modifier.weight(1f))
                    LitoGenNumberField(R.string.LitoGenHeight, heightText, { heightText = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LitoGenNumberField(R.string.LitoGenMinThickness, minThicknessText, { minThicknessText = it }, Modifier.weight(1f))
                    LitoGenNumberField(R.string.LitoGenMaxThickness, maxThicknessText, { maxThicknessText = it }, Modifier.weight(1f))
                }
            } else {
                LitoGenNumberField(R.string.LitoGenWidth, widthText, { widthText = it }, Modifier.fillMaxWidth())
                LitoGenNumberField(R.string.LitoGenHeight, heightText, { heightText = it }, Modifier.fillMaxWidth())
                LitoGenNumberField(R.string.LitoGenMinThickness, minThicknessText, { minThicknessText = it }, Modifier.fillMaxWidth())
                LitoGenNumberField(R.string.LitoGenMaxThickness, maxThicknessText, { maxThicknessText = it }, Modifier.fillMaxWidth())
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.LitoGenBrightnessValue, brightness),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { brightness = it.roundToInt().coerceIn(-50, 50) },
                    valueRange = -50f..50f,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.LitoGenContrastValue, contrast),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Slider(
                    value = contrast.toFloat(),
                    onValueChange = { contrast = it.roundToInt().coerceIn(50, 180) },
                    valueRange = 50f..180f,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = invert, onCheckedChange = { invert = it })
                Text(
                    text = stringResource(R.string.LitoGenInvert),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            var detailsExpanded by remember(images) { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { detailsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${stringResource(R.string.LithophaneResolution)}: ${details.getOrNull(detailIndex).orEmpty()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(expanded = detailsExpanded, onDismissRequest = { detailsExpanded = false }) {
                    details.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { detailIndex = index; detailsExpanded = false },
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.LitoGenFitHelp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }

        validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    validationError = onGenerate(
                        readOptions(widthText, heightText, minThicknessText, maxThicknessText, brightness, contrast, invert, detailIndex),
                        false,
                    )
                },
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            ) {
                Text(stringResource(R.string.LithophaneGenerate), fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
            OutlinedButton(
                onClick = {
                    validationError = onGenerate(
                        readOptions(widthText, heightText, minThicknessText, maxThicknessText, brightness, contrast, invert, detailIndex),
                        true,
                    )
                },
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
            ) {
                Text(stringResource(R.string.LithophaneGenerateAndSave), fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun LitoGenImagePreviews(
    activity: Activity,
    images: List<Uri>,
    previewFilter: ColorFilter,
) {
    val imageNames = remember(images) { images.map { LitoGenGenerator.displayName(activity, it) } }
    val thumbnails = remember(images) {
        mutableStateListOf<Bitmap?>().apply { repeat(images.size) { add(null) } }
    }
    DisposableEffect(images) {
        val active = AtomicBoolean(true)
        images.forEachIndexed { index, uri ->
            IOUtils.IO_POOL.execute {
                val thumbnail = try {
                    ImageToStlConverter.decodeBitmap(activity.contentResolver, uri, 160)
                } catch (_: Exception) {
                    null
                }
                ViewUtils.postOnMainThread {
                    if (active.get() && thumbnail != null) thumbnails[index] = thumbnail
                    else thumbnail?.recycle()
                }
            }
        }
        onDispose {
            active.set(false)
            thumbnails.forEach { it?.recycle() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        images.forEachIndexed { index, uri ->
            val name = imageNames[index]
            val label = LitoGenGenerator.imageLabel(activity, index)
            Column(
                modifier = Modifier.size(width = 104.dp, height = 128.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val thumbnail = thumbnails.getOrNull(index)
                if (thumbnail == null) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                } else {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "$label: $name",
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
                        contentScale = ContentScale.Crop,
                        colorFilter = previewFilter,
                    )
                }
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LitoGenNumberField(label: Int, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(stringResource(label), maxLines = 3, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun readOptions(
    width: String,
    height: String,
    minThickness: String,
    maxThickness: String,
    brightness: Int,
    contrast: Int,
    invert: Boolean,
    detailIndex: Int,
) = LitoGenOptions(
    widthMm = LitoGenFormLogic.parseNumber(width),
    heightMm = LitoGenFormLogic.parseNumber(height),
    minThicknessMm = LitoGenFormLogic.parseNumber(minThickness),
    maxThicknessMm = LitoGenFormLogic.parseNumber(maxThickness),
    brightness = brightness,
    contrast = contrast,
    invert = invert,
    detailIndex = detailIndex,
)

private fun litoGenPreviewFilter(brightness: Int, contrast: Int, invert: Boolean): ColorFilter {
    var scale = contrast / 100f
    var offset = (0.5f * (1f - scale) + brightness / 100f) * 255f
    if (invert) {
        scale = -scale
        offset = 255f - offset
    }
    val red = 0.2126f * scale
    val green = 0.7152f * scale
    val blue = 0.0722f * scale
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                red, green, blue, 0f, offset,
                red, green, blue, 0f, offset,
                red, green, blue, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
