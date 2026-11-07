package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceChoiceField
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.FastenerStandards
import ru.ytkab0bp.slicebeam.utils.FastenerStlWriter
import ru.ytkab0bp.slicebeam.utils.IOUtils
import java.io.File
import java.util.Locale
import kotlin.math.max

/** Touch-friendly local generator for printable metric bolts, threaded nuts and washers. */
object FastenerGenerator {
    @JvmStatic
    fun show(activity: Activity, bed: BedFragment) {
        open(activity, bed)
    }

    private fun open(activity: Activity, bed: BedFragment) {
        val form = mutableStateOf(FastenerFormLogic.defaults())
        val error = mutableStateOf<String?>(null)
        bed.showWorkspaceComposeDialog { dismiss ->
            GeneratorWorkspaceDialog(R.string.FastenerTitle, onDismiss = dismiss) {
                FastenerFormContent(
                    values = form.value,
                    errorMessage = error.value,
                    onValuesChanged = {
                        form.value = it
                        error.value = null
                    },
                    onGenerate = { values ->
                        val issue = FastenerFormLogic.validate(values)
                        if (issue != null) {
                            error.value = issue
                        } else {
                            dismiss()
                            generate(
                                activity,
                                bed,
                                values.type,
                                values.size,
                                FastenerFormLogic.parse(values.first),
                                FastenerFormLogic.parse(values.second),
                                FastenerFormLogic.parse(values.third),
                                FastenerFormLogic.parse(values.fourth),
                            )
                        }
                    },
                    onFitSample = { values ->
                        val issue = FastenerFormLogic.validateFitSample(values)
                        if (issue != null) {
                            error.value = issue
                        } else {
                            dismiss()
                            generateFitSample(
                                activity,
                                bed,
                                values.size,
                                FastenerFormLogic.parse(values.second),
                                FastenerFormLogic.parse(values.fourth),
                                FastenerFormLogic.parse(values.first),
                            )
                        }
                    },
                )
            }
        }
    }

    private fun generate(activity: Activity, bed: BedFragment, type: Int, size: Int,
                         a: Float, b: Float, c: Float, d: Float) {
        val nominal = FastenerStandards.diameter(size)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val part = if (type == 0) "schraube" else if (type == 1) "mutter" else "scheibe"
            val nominalName = "M" + FastenerStandards.fileNumber(nominal)
            val dimensions = if (type == 0) "-${FastenerStandards.fileNumber(a)}mm"
                else if (type == 2) "-A${FastenerStandards.fileNumber(a)}-I${FastenerStandards.fileNumber(b)}-D${FastenerStandards.fileNumber(c)}mm"
                else ""
            val output = File(activity.cacheDir, "OrkanSlicer-$part-$nominalName$dimensions-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(15, activity.getString(R.string.FastenerBuilding))
            try {
                when (type) {
                    0 -> FastenerStlWriter.writeBolt(output, nominal, b, a, d, c)
                    1 -> FastenerStlWriter.writeThreadedNut(output, nominal, b, d, a, c)
                    else -> FastenerStlWriter.writeWasher(output, a, b, c)
                }
                progress.setStage(92, activity.getString(R.string.FastenerChecking))
                progress.complete { ImageToStlConverter.saveGeneratedStl(activity, output, bed) }
            } catch (error: Exception) {
                progress.fail()
                if (output.exists()) output.delete()
                showGeneratorFailure(activity, bed, error.message ?: error.toString())
            }
        }
    }

    private fun generateFitSample(activity: Activity, bed: BedFragment, size: Int,
                                  pitch: Float, acrossFlats: Float, nutHeight: Float) {
        val diameter = FastenerStandards.diameter(size)
        val headHeight = max(1.2f, diameter * .67f)
        val progress = ConverterProgressDialog.show(activity, activity.getString(R.string.ConverterWorking), bed)
        IOUtils.IO_POOL.execute {
            val output = File(activity.cacheDir,
                "OrkanSlicer-Passprobe-M${FastenerStandards.fileNumber(diameter)}-${System.currentTimeMillis()}.stl")
            progress.setOutputFile(output)
            progress.setStage(12, activity.getString(R.string.FastenerBuilding))
            try {
                FastenerStlWriter.writeThreadFitCoupon(output, diameter, pitch, 16f, acrossFlats, headHeight, nutHeight)
                progress.setStage(92, activity.getString(R.string.FastenerChecking))
                progress.complete { ImageToStlConverter.saveGeneratedStl(activity, output, bed) }
            } catch (error: Exception) {
                progress.fail()
                if (output.exists()) output.delete()
                showGeneratorFailure(activity, bed, error.message ?: error.toString())
            }
        }
    }
}

@Composable
private fun FastenerFormContent(
    values: FastenerFormValues,
    errorMessage: String?,
    onValuesChanged: (FastenerFormValues) -> Unit,
    onGenerate: (FastenerFormValues) -> Unit,
    onFitSample: (FastenerFormValues) -> Unit,
) {
    // Leave room for the workspace dialog title and Back/Cancel actions on short screens.
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.68f
    val types = stringArrayResource(R.array.FastenerTypes).toList()
    val sizes = stringArrayResource(R.array.FastenerSizes).toList()
    val fits = stringArrayResource(R.array.FastenerThreadClearancePresets).toList()
    val hint = when (values.type) {
        0 -> R.string.FastenerBoltHint
        1 -> R.string.FastenerNutHint
        else -> R.string.FastenerWasherHint
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        SliceChoiceField(
            label = stringResource(R.string.FastenerKind),
            selectedIndex = values.type,
            options = types,
            onSelected = { onValuesChanged(FastenerFormLogic.selectType(it, values.size)) },
        )
        SliceChoiceField(
            label = stringResource(R.string.FastenerSize),
            selectedIndex = values.size,
            options = sizes,
            onSelected = { onValuesChanged(FastenerFormLogic.selectSize(values.type, it)) },
        )

        when (values.type) {
            0 -> {
                DimensionField(R.string.FastenerLength, values.first, { onValuesChanged(values.copy(first = it)) })
                DimensionField(R.string.FastenerPitch, values.second, { onValuesChanged(values.copy(second = it)) })
                DimensionField(R.string.FastenerHeadHeight, values.third, { onValuesChanged(values.copy(third = it)) })
                DimensionField(R.string.FastenerAcrossFlats, values.fourth, { onValuesChanged(values.copy(fourth = it)) })
            }
            1 -> {
                DimensionField(R.string.FastenerHeight, values.first, { onValuesChanged(values.copy(first = it)) })
                DimensionField(R.string.FastenerPitch, values.second, { onValuesChanged(values.copy(second = it)) })
                SliceChoiceField(
                    label = stringResource(R.string.FastenerThreadFitPreset),
                    selectedIndex = values.fitPreset,
                    options = fits,
                    onSelected = { onValuesChanged(FastenerFormLogic.selectFitPreset(values, it)) },
                )
                DimensionField(R.string.FastenerThreadClearance, values.third, { onValuesChanged(values.copy(third = it)) })
                DimensionField(R.string.FastenerAcrossFlats, values.fourth, { onValuesChanged(values.copy(fourth = it)) })
                Text(
                    text = stringResource(R.string.FastenerThreadHelp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    text = stringResource(R.string.FastenerFitSampleHint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            else -> {
                DimensionField(R.string.FastenerOuterDiameter, values.first, { onValuesChanged(values.copy(first = it)) })
                DimensionField(R.string.FastenerBore, values.second, { onValuesChanged(values.copy(second = it)) })
                DimensionField(R.string.FastenerThickness, values.third, { onValuesChanged(values.copy(third = it)) })
                Text(
                    text = stringResource(R.string.FastenerFitHelp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        if (values.type == 0) {
            Text(
                text = stringResource(R.string.FastenerThreadHelp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (values.type == 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onFitSample(values) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.FastenerGenerateFitSample), lineHeight = 19.sp)
                }
                Button(
                    onClick = { onGenerate(values) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.FastenerGenerate), lineHeight = 18.sp)
                }
            }
        } else {
            Button(
                onClick = { onGenerate(values) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.FastenerGenerate), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun DimensionField(label: Int, value: String, onValueChanged: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        shape = MaterialTheme.shapes.medium,
    )
}

internal data class FastenerFormValues(
    val type: Int,
    val size: Int,
    val first: String,
    val second: String,
    val third: String,
    val fourth: String,
    val fitPreset: Int,
)

/** Pure form state keeps metric defaults and dimension constraints covered without Android views. */
internal object FastenerFormLogic {
    fun defaults(type: Int = 0, size: Int = 0): FastenerFormValues = create(type, size)

    fun selectType(type: Int, size: Int): FastenerFormValues = create(type, size)

    fun selectSize(type: Int, size: Int): FastenerFormValues = create(type, size)

    fun selectFitPreset(values: FastenerFormValues, fitPreset: Int): FastenerFormValues {
        val fit = fitPreset.coerceIn(0, 2)
        return values.copy(third = format(FastenerStandards.threadClearance(fit)), fitPreset = fit)
    }

    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validateFitSample(values: FastenerFormValues): String? = validate(
        values.copy(third = format(FastenerStandards.threadClearance(2))),
    )

    fun validate(values: FastenerFormValues): String? {
        if (values.type !in 0..2) return "Wähle Schraube, Mutter oder Scheibe aus."
        if (values.size !in 0 until FastenerStandards.count()) return "Wähle eine metrische Größe aus."
        val a = parse(values.first)
        val b = parse(values.second)
        val c = parse(values.third)
        val d = parse(values.fourth).let { if (values.type == 2) 0f else it }
        val nominal = FastenerStandards.diameter(values.size)
        if (values.type == 0) {
            if (!range(a, 3f, 160f)) return "Die Schraubenlänge muss zwischen 3 und 160 mm liegen."
            if (!range(b, .25f, 3f) || nominal - 1.08f * b <= .4f) return "Die Gewindesteigung passt nicht zur gewählten Größe."
            if (!range(c, nominal * .45f, nominal * 1.8f)) return "Die Kopfhöhe passt nicht zur gewählten Größe."
            if (!range(d, nominal * 1.4f, nominal * 3.5f)) return "Die Schlüsselweite passt nicht zur gewählten Größe."
        } else if (values.type == 1) {
            if (!range(a, 1.2f, 24f) || !range(b, .25f, 3f) || !range(c, .05f, .7f) ||
                !range(d, nominal * 1.4f, nominal * 3.5f)) return "Mutternmaße liegen außerhalb des zulässigen Bereichs."
            if (nominal - 1.08f * b <= .4f) return "Die Gewindesteigung passt nicht zur gewählten Größe."
            if (d - nominal - c * 2f < 1.3f) return "Die Sechskantwand ist zu dünn. Vergrößere die Schlüsselweite oder verringere das Spiel."
        } else {
            if (!range(a, 3f, 80f) || !range(c, .6f, 12f)) return "Außendurchmesser oder Dicke liegt außerhalb des zulässigen Bereichs."
            if (!range(b, 1f, a - .8f)) return "Der Scheibenrand muss mindestens 0,4 mm breit sein."
        }
        return null
    }

    private fun create(type: Int, size: Int): FastenerFormValues {
        val safeType = type.coerceIn(0, 2)
        val safeSize = size.coerceIn(0, FastenerStandards.count() - 1)
        val diameter = FastenerStandards.diameter(safeSize)
        val flats = format(FastenerStandards.hexNutAcrossFlats(safeSize))
        return when (safeType) {
            0 -> FastenerFormValues(
                type = safeType,
                size = safeSize,
                first = if (safeSize <= 1) "12" else if (safeSize <= 4) "16" else if (safeSize <= 6) "20" else "30",
                second = format(FastenerStandards.coarsePitch(safeSize)),
                third = format(max(1.2f, diameter * .67f)),
                fourth = flats,
                fitPreset = 1,
            )
            1 -> FastenerFormValues(
                type = safeType,
                size = safeSize,
                first = format(FastenerStandards.nutHeight(safeSize)),
                second = format(FastenerStandards.coarsePitch(safeSize)),
                third = format(FastenerStandards.threadClearance(1)),
                fourth = flats,
                fitPreset = 1,
            )
            else -> FastenerFormValues(
                type = safeType,
                size = safeSize,
                first = format(FastenerStandards.washerOuterDiameter(safeSize)),
                second = format(FastenerStandards.clearanceBore(safeSize)),
                third = format(FastenerStandards.washerThickness(safeSize)),
                fourth = "",
                fitPreset = 1,
            )
        }
    }

    private fun range(value: Float, min: Float, max: Float): Boolean =
        value.isFinite() && min <= max && value >= min && value <= max

    private fun format(value: Float): String = String.format(Locale.US, "%.2f", value)
        .replace(Regex("0+$"), "")
        .replace(Regex("\\.$"), "")
}
