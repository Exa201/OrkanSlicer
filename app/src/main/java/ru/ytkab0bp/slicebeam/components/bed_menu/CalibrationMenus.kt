package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.events.NeedDismissCalibrationsMenu
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File
import java.io.FileOutputStream

/** Calibration actions and test models opened from the File menu. */
class CalibrationsMenu : UnfoldMenu() {
    override fun getRequestedSize(into: android.widget.FrameLayout, portrait: Boolean): Int =
        (if (portrait) into.height * 0.35f else into.width * 0.6f).toInt()

    override fun onCreateView(ctx: Context, portrait: Boolean): View {
        val bed = fragment
        return ThemeAwareComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme(themeRevision = themeRevision) {
                    if (bed == null) {
                        Spacer(Modifier.fillMaxSize())
                    } else {
                        CalibrationsMenuContent(
                            onPressureAdvance = { startPressureAdvanceCalibration(ctx, bed) },
                            onModels = {
                                dismiss(true)
                                ViewUtils.postOnMainThread({
                                    bed.glView?.let { bed.showUnfoldMenu(CalibrationModelsMenu(), it) }
                                }, 180)
                            },
                            onBack = { dismiss() },
                        )
                    }
                }
            }
        }
    }

    internal fun startPressureAdvanceCalibration(context: Context, bed: BedFragment) {
        SliceBeam.PENDING_CALIB_MODE = 1 // CalibMode::Calib_PA_Line
        SliceBeam.PENDING_CALIB_START = 0.0
        SliceBeam.PENDING_CALIB_END = 0.1
        SliceBeam.PENDING_CALIB_STEP = 0.002

        val model = bed.glView!!.renderer.model
        if (model == null || model.objectsCount == 0) {
            ensurePlaceholderModel(bed) {
                finishPressureAdvanceSelection(context)
            }
        } else {
            finishPressureAdvanceSelection(context)
        }
    }

    private fun finishPressureAdvanceSelection(context: Context) {
        Toast.makeText(context, R.string.CalibrationPressureAdvanceReady, Toast.LENGTH_LONG).show()
        SliceBeam.EVENT_BUS.fireEvent(NeedDismissCalibrationsMenu())
        dismiss(true)
    }

    private fun ensurePlaceholderModel(bed: BedFragment, onLoaded: () -> Unit) {
        val file = File(SliceBeam.getModelCacheDir(), "calibration_pa_test.stl")
        Thread({
            try {
                SliceBeam.requireInstance().assets.open("models/pa_test.stl").use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                ViewUtils.postOnMainThread {
                    bed.loadModel(file, false, 1, object : BedFragment.ModelLoadCallback {
                        override fun onLoaded(model: ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject: Int, addedObjects: Int) {
                            ViewUtils.postOnMainThread {
                                SliceBeam.EVENT_BUS.fireEvent(ObjectsListChangedEvent())
                                onLoaded()
                            }
                        }

                        override fun onFailed(error: Exception) {
                            showLoadError(bed, error)
                        }
                    })
                }
            } catch (error: Exception) {
                android.util.Log.e("FileMenu", "Failed to load PA placeholder model", error)
                ViewUtils.postOnMainThread { showLoadError(bed, error) }
            }
        }, "CalibrationPlaceholderLoader").start()
    }

    private fun showLoadError(bed: BedFragment, error: Exception) {
        val context = bed.getContext() ?: return
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.MenuFileOpenFileFailed)
            .setMessage(error.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @EventHandler(runOnMainThread = true)
    fun onDismiss(event: NeedDismissCalibrationsMenu) {
        dismiss()
    }

    override fun onCreate() {
        super.onCreate()
        SliceBeam.EVENT_BUS.registerListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SliceBeam.EVENT_BUS.unregisterListener(this)
    }
}

class CalibrationModelsMenu : UnfoldMenu() {
    override fun getRequestedSize(into: android.widget.FrameLayout, portrait: Boolean): Int =
        if (portrait) into.height - into.paddingTop - into.paddingBottom else into.width

    override fun onCreateView(ctx: Context, portrait: Boolean): View {
        val bed = fragment
        val models = listOf(
            CalibrationModel("Orca Cube", R.drawable.model_thumb_orcacube_v2, listOf("OrcaCube_v2.stl", "OrcaPlug_v2.stl")),
            CalibrationModel("Orca Tolerance Test", R.drawable.model_thumb_orcatolerancetest, listOf("OrcaToleranceTest.stl")),
            CalibrationModel(ctx.getString(R.string.MenuFileCalibrationsModels3DBenchy), R.drawable.model_thumb_3dbenchy, listOf("3dbenchy.stl")),
            CalibrationModel("Cali Cat", R.drawable.model_thumb_calicat, listOf("calicat.stl")),
            CalibrationModel("Autodesk FDM Test", R.drawable.model_thumb_ksr_fdmtest_v4, listOf("ksr_fdmtest_v4.stl")),
            CalibrationModel("Voron Cube", R.drawable.model_thumb_voron_design_cube_v7, listOf("Voron_Design_Cube_v7.stl")),
            CalibrationModel("Stanford Bunny", R.drawable.model_thumb_stanford_bunny, listOf("Stanford_Bunny.stl")),
            CalibrationModel("Orca String Hell", R.drawable.model_thumb_orca_stringhell, listOf("Orca_stringhell.stl")),
        )
        return ThemeAwareComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme(themeRevision = themeRevision) {
                    if (bed == null) {
                        Spacer(Modifier.fillMaxSize())
                    } else {
                        CalibrationModelsContent(
                            models = models,
                            onBack = { dismiss() },
                            onSelect = { model -> loadModels(bed, model.assets) },
                        )
                    }
                }
            }
        }
    }

    private fun loadModels(bed: BedFragment, assetNames: List<String>) {
        SliceBeam.EVENT_BUS.fireEvent(NeedDismissCalibrationsMenu())
        dismiss(true)
        ViewUtils.postOnMainThread({
            Thread({
                val files = mutableListOf<File>()
                try {
                    assetNames.forEach { assetName ->
                        val file = File(SliceBeam.getModelCacheDir(), "calibration_model_$assetName")
                        SliceBeam.requireInstance().assets.open("models/$assetName").use { input ->
                            FileOutputStream(file).use { output -> input.copyTo(output) }
                        }
                        files += file
                    }
                    ViewUtils.postOnMainThread { loadNextModel(bed, files, 0) }
                } catch (error: Exception) {
                    files.forEach(File::delete)
                    ViewUtils.postOnMainThread { showLoadError(bed, error) }
                }
            }, "CalibrationModelAssetLoader").start()
        }, 200)
    }

    /** Loads multi-part presets one at a time; BedFragment rejects overlapping native imports. */
    private fun loadNextModel(bed: BedFragment, files: List<File>, index: Int) {
        if (index >= files.size) {
            files.forEach(File::delete)
            SliceBeam.EVENT_BUS.fireEvent(ObjectsListChangedEvent())
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuFileOpenFileLoaded))
            return
        }
        try {
            bed.loadModel(files[index], false, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(model: ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject: Int, addedObjects: Int) {
                    ViewUtils.postOnMainThread { loadNextModel(bed, files, index + 1) }
                }

                override fun onFailed(error: Exception) {
                    files.forEach(File::delete)
                    showLoadError(bed, error)
                }
            })
        } catch (error: Exception) {
            files.forEach(File::delete)
            showLoadError(bed, error)
        }
    }

    private fun showLoadError(bed: BedFragment, error: Exception) {
        val context = bed.getContext() ?: return
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.MenuFileOpenFileFailed)
            .setMessage(error.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

}

private data class CalibrationModel(val title: String, val icon: Int, val assets: List<String>)

@Composable
private fun CalibrationsMenuContent(
    onPressureAdvance: () -> Unit,
    onModels: (View) -> Unit,
    onBack: () -> Unit,
) {
    val anchor = LocalView.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalibrationActionRow(
                icon = R.drawable.menu_calibrate_la_28,
                title = stringResource(R.string.CalibrationPressureAdvance),
                subtitle = stringResource(R.string.CalibrationPressureAdvanceDescription),
                onClick = onPressureAdvance,
            )
            CalibrationActionRow(
                icon = R.drawable.deployed_code_24,
                title = stringResource(R.string.MenuFileCalibrationsModels),
                subtitle = stringResource(R.string.MenuFileCalibrationsModelsDescription),
                onClick = { onModels(anchor) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CalibrationBackBar(onBack)
    }
}

@Composable
private fun CalibrationActionRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CalibrationModelsContent(
    models: List<CalibrationModel>,
    onBack: () -> Unit,
    onSelect: (CalibrationModel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CalibrationBackBar(onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(models) { model ->
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onSelect(model) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(model.icon),
                            contentDescription = model.title,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)),
                        )
                        Text(
                            text = model.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationBackBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.MenuOrientationPositionBack), onClick = onBack)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.arrow_left_outline_28),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.MenuOrientationPositionBack),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
