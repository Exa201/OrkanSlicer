package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.BeamServerData
import ru.ytkab0bp.slicebeam.BuildConfig
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.launchAppActivityResult
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SetupActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.cloud.CloudController
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.events.CloudFeaturesUpdatedEvent
import ru.ytkab0bp.slicebeam.events.CloudModelsRemainingCountUpdatedEvent
import ru.ytkab0bp.slicebeam.events.FlattenModeResetEvent
import ru.ytkab0bp.slicebeam.events.NeedDismissAIGeneratorMenu
import ru.ytkab0bp.slicebeam.events.NeedDismissSnackbarEvent
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout
import java.io.File
import java.io.IOException

internal fun aiGenerationQuotaProgress(total: Int, generated: Int): Float =
    if (total <= 0) 0f else ((total - generated).toFloat() / total).coerceIn(0f, 1f)

/** File, object and profile actions for the model workspace. */
class FileMenu : ListBedMenu() {
    private var wasPortrait = false
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var modelMenuItems: ModelMenuItems? = null

    override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView {
        wasPortrait = portrait
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        modelMenuItems = fragment?.let { ModelMenuItems(it, ::refreshComposeActions) }
        composeActions.value = createComposeActions(portrait)
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                BedMenuActionsContent(composeActions.value, portrait, multiRow = true)
            }
        }
        return themedView
    }

    private fun createComposeActions(portrait: Boolean): List<BedComposeMenuAction> =
        onCreateItems(portrait).map { menuItem ->
            BedComposeMenuAction(
                titleRes = menuItem.titleRes,
                iconRes = menuItem.iconRes,
                onClick = { anchor ->
                    if (menuItem.isCheckable) {
                        menuItem.isChecked = !menuItem.isChecked
                        menuItem.checkedChangeListener?.onCheckedChanged(menuItem.isChecked)
                        refreshComposeActions()
                    } else {
                        menuItem.clickListener?.invoke(anchor)
                    }
                },
                selected = menuItem.isCheckable && menuItem.isChecked,
                toggleable = menuItem.isCheckable,
                sectionBreakBefore = menuItem.sectionBreakBefore,
                enabled = menuItem.isEnabled,
            )
        } + modelMenuItems?.createItems().orEmpty()

    private fun refreshComposeActions() {
        composeActions.value = createComposeActions(wasPortrait)
    }

    private fun hasModel(): Boolean =
        fragment?.glView?.renderer?.model != null

    private fun hasSelection(): Boolean {
        val bed = fragment ?: return false
        return bed.glView!!.renderer.model != null &&
            bed.glView!!.renderer.selectedObject != -1
    }

    private fun hasObjects(): Boolean =
        (fragment?.glView?.renderer?.model?.objectsCount ?: 0) > 0

    private fun showPositionMenu(view: View) {
        val bed = fragment ?: return
        bed.showUnfoldMenu(OrientationPositionMenu(bed), view)
    }

    private fun showRotationMenu(view: View) {
        val bed = fragment ?: return
        bed.showUnfoldMenu(OrientationRotationMenu(bed), view)
    }

    private fun showScaleMenu(view: View) {
        val bed = fragment ?: return
        bed.showUnfoldMenu(TransformScaleMenu(bed), view)
    }

    private fun openModel(view: View) {
        val bed = fragment ?: return
        if (!bed.glView!!.renderer.bed!!.isValid()) {
            Toast.makeText(bed.getContext(), R.string.BedConfigurationError, Toast.LENGTH_SHORT).show()
            return
        }
        val activity = bed.getContext() as? Activity ?: return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_OPEN_FILE)
    }

    private fun objectActions(): FileMenuObjectActions? =
        fragment?.let(::FileMenuObjectActions)

    private fun showCopyMoveDialog(view: View) = objectActions()?.showCopyMoveDialog(view)

    private fun showObjectsDialog(view: View) = objectActions()?.showObjectsDialog(view)

    private fun showSelectedObjectActions(view: View) = objectActions()?.showSelectedObjectActions(view)

    /** Assigns a whole object to an ACE/slicer tool; fine-grained color painting stays separate. */
    private fun showObjectFilamentSlotDialog(view: View) = objectActions()?.showObjectFilamentSlotDialog(view)

    private fun showDuplicateDialog(view: View) = objectActions()?.showDuplicateDialog(view)

    private fun showMoveToPlateDialog(view: View) = objectActions()?.showMoveToPlateDialog(view)

    /** Additional workspace actions shown directly in the Options destination. */
    internal fun createOptionsItems(): List<BedMenuItem> {
        val bed = fragment ?: return emptyList()
        val items = mutableListOf<BedMenuItem>()

        if (BeamServerData.isBoostyAvailable() && CloudController.needShowAIGenerator()) {
            // A static icon avoids the old sparkle animation invalidating the menu continuously.
            items += BedMenuItem(R.string.MenuFileAIGenerator, R.drawable.picture_stack_outline_28)
                .onClick { view ->
                    if (Prefs.getCloudAPIToken() == null ||
                        (CloudController.getUserInfo() != null && CloudController.getMaxGeneratedModels() == 0)
                    ) {
                        view.context.startActivity(
                            Intent(view.context, SetupActivity::class.java)
                                .putExtra(SetupActivity.EXTRA_CLOUD_PROFILE, true),
                        )
                        return@onClick
                    }
                    if (CloudController.getUserInfo() == null) {
                        SliceBeam.EVENT_BUS.fireEvent(
                            NeedSnackbarEvent(
                                SnackbarsLayout.Type.LOADING,
                                R.string.MenuFileAIGeneratorPleaseWaitSetup,
                            ).tag(CloudController.USER_INFO_AI_GEN_TAG),
                        )
                        ViewUtils.postOnMainThread({
                            if (CloudController.getUserInfo() == null) {
                                SliceBeam.EVENT_BUS.fireEvent(
                                    NeedDismissSnackbarEvent(CloudController.USER_INFO_AI_GEN_TAG),
                                )
                                SliceBeam.EVENT_BUS.fireEvent(
                                    NeedSnackbarEvent(
                                        SnackbarsLayout.Type.ERROR,
                                        R.string.MenuFileAIGeneratorErrorNotLoadedUserAccount,
                                    ),
                                )
                            } else {
                                bed.showUnfoldMenu(AIGeneratorMenu(), view)
                            }
                        }, 2500)
                        return@onClick
                    }
                    bed.showUnfoldMenu(AIGeneratorMenu(), view)
                }
        }

        items += BedMenuItem(R.string.MenuFileCalibrations, R.drawable.menu_calibrate_la_28)
            .onClick { view -> bed.showUnfoldMenu(CalibrationsMenu(), view) }
        items += BedMenuItem(R.string.MenuFileImportProfiles, R.drawable.folder_simple_arrow_up_outline_28)
            .onClick {
                val activity = bed.getContext() as? Activity ?: return@onClick
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "text/plain"),
                    )
                }
                activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_IMPORT_PROFILES)
            }
        items += BedMenuItem(R.string.MenuFileExportProfiles, R.drawable.folder_simple_arrow_right_outline_28)
            .onClick(::showProfileExportDialog)
        return items
    }

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> {
        val bed = fragment ?: return emptyList()
        wasPortrait = portrait
        return listOf(
            BedMenuItem(R.string.WorkspaceAddModelAction, R.drawable.folder_simple_plus_outline_28)
                .onClick(::openModel),
            BedMenuItem(R.string.MenuFileExport3mf, R.drawable.arrow_down_to_square_outline_28)
                .setEnabled(hasObjects())
                .onClick {
                    val activity = bed.getContext() as? Activity ?: return@onClick
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = "application/3mf"
                        putExtra(Intent.EXTRA_TITLE, "OrkanSlicer_project.3mf")
                    }
                    activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_EXPORT_3MF)
                },
        )
    }

    private fun showProfileExportDialog(view: View) {
        val prints = Array<CharSequence>(SliceBeam.requireConfig().printConfigs.size) { index ->
            SliceBeam.requireConfig().printConfigs[index].title ?: ""
        }
        val filaments = Array<CharSequence>(SliceBeam.requireConfig().filamentConfigs.size) { index ->
            SliceBeam.requireConfig().filamentConfigs[index].title ?: ""
        }
        val printers = Array<CharSequence>(SliceBeam.requireConfig().printerConfigs.size) { index ->
            SliceBeam.requireConfig().printerConfigs[index].title ?: ""
        }
        val enabledPrints = BooleanArray(prints.size) { true }
        val enabledFilaments = BooleanArray(filaments.size) { true }
        val enabledPrinters = BooleanArray(printers.size) { true }

        BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.MenuFileExportProfilesPrints)
            .setMultiChoiceItems(prints, enabledPrints) { _, which, checked -> enabledPrints[which] = checked }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                BeamAlertDialogBuilder(view.context)
                    .setTitle(R.string.MenuFileExportProfilesFilaments)
                    .setMultiChoiceItems(filaments, enabledFilaments) { _, which, checked ->
                        enabledFilaments[which] = checked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        BeamAlertDialogBuilder(view.context)
                            .setTitle(R.string.MenuFileExportProfilesPrinters)
                            .setMultiChoiceItems(printers, enabledPrinters) { _, which, checked ->
                                enabledPrinters[which] = checked
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val selectedPrints = SliceBeam.requireConfig().printConfigs.filterIndexed { index, _ ->
                                    enabledPrints[index]
                                }
                                val selectedFilaments = SliceBeam.requireConfig().filamentConfigs.filterIndexed { index, _ ->
                                    enabledFilaments[index]
                                }
                                val selectedPrinters = SliceBeam.requireConfig().printerConfigs.filterIndexed { index, _ ->
                                    enabledPrinters[index]
                                }
                                if (selectedPrints.isEmpty() && selectedFilaments.isEmpty() && selectedPrinters.isEmpty()) {
                                    BeamAlertDialogBuilder(view.context)
                                        .setTitle(R.string.MenuFileExportProfiles)
                                        .setMessage(R.string.MenuFileExportProfilesNoProfiles)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show()
                                    return@setPositiveButton
                                }

                                MainActivity.EXPORTING_PRINTS = selectedPrints
                                MainActivity.EXPORTING_FILAMENTS = selectedFilaments
                                MainActivity.EXPORTING_PRINTERS = selectedPrinters
                                val activity = fragment?.getContext() as? Activity ?: return@setPositiveButton
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = "application/ini"
                                    putExtra(Intent.EXTRA_TITLE, "OrcaSlicerMobile_config_bundle.ini")
                                }
                                activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_EXPORT_PROFILES)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @EventHandler(runOnMainThread = true)
    fun onObjectsChanged(event: ObjectsListChangedEvent) {
        modelMenuItems?.resetFlattenMode()
        refreshComposeActions()
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectionChanged(event: SelectedObjectChangedEvent) {
        modelMenuItems?.resetFlattenMode()
        refreshComposeActions()
    }

    @EventHandler(runOnMainThread = true)
    fun onFlattenModeReset(event: FlattenModeResetEvent) {
        modelMenuItems?.resetFlattenMode()
        refreshComposeActions()
    }

    @EventHandler(runOnMainThread = true)
    fun onFeaturedUpdated(event: CloudFeaturesUpdatedEvent) {
        refreshComposeActions()
    }

    /** Camera, gallery and quota controls for the optional cloud model generator. */
    class AIGeneratorMenu : UnfoldMenu() {
        private var remainingLabel by mutableStateOf("")
        private var quotaProgress by mutableFloatStateOf(0f)

        override fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
            if (portrait) {
                ViewUtils.dp(52) + ViewUtils.dp(60) * 2 + ViewUtils.dp(28) + ViewUtils.dp(18) + ViewUtils.dp(2)
            } else {
                (into.width * 0.6f).toInt()
            }

        override fun onCreateView(ctx: Context, portrait: Boolean): View {
            val themedView = ThemeAwareComposeView(ctx)
            themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            themedView.setContent {
                BeamComposeTheme(themeRevision = themedView.themeRevision) {
                    AIGeneratorMenuContent(
                        remainingLabel = remainingLabel,
                        quotaProgress = quotaProgress,
                        onCamera = { openCamera(ctx) },
                        onGallery = { openGallery(ctx) },
                        onBack = { dismiss() },
                    )
                }
            }
            return themedView
        }

        override fun onCreate() {
            super.onCreate()
            SliceBeam.EVENT_BUS.registerListener(this)
            updateRemaining()
        }

        @EventHandler(runOnMainThread = true)
        fun onDismiss(event: NeedDismissAIGeneratorMenu) {
            dismiss()
        }

        @EventHandler(runOnMainThread = true)
        fun onRemainingUpdated(event: CloudModelsRemainingCountUpdatedEvent) {
            updateRemaining()
        }

        override fun onDestroy() {
            super.onDestroy()
            SliceBeam.EVENT_BUS.unregisterListener(this)
        }

        private fun updateRemaining() {
            val total = CloudController.getMaxGeneratedModels()
            val generated = CloudController.getGeneratedModels()
            remainingLabel = SliceBeam.requireInstance().getResources().getQuantityString(
                R.plurals.MenuFileAIGeneratorRemaining,
                total,
                (total - generated).coerceAtLeast(0),
                total,
            )
            quotaProgress = aiGenerationQuotaProgress(total, generated)
        }

        private fun canStartGeneration(): Boolean {
            if (CloudController.getGeneratedModels() >= CloudController.getMaxGeneratedModels()) {
                SliceBeam.EVENT_BUS.fireEvent(
                    NeedSnackbarEvent(
                        SnackbarsLayout.Type.ERROR,
                        R.string.MenuFileAIGeneratorNoGenerationsLeft,
                    ),
                )
                return false
            }
            if (MainActivity.IS_GENERATING_AI_MODEL) {
                SliceBeam.EVENT_BUS.fireEvent(
                    NeedSnackbarEvent(
                        SnackbarsLayout.Type.WARNING,
                        R.string.MenuFileAIGeneratorAlreadyGenerating,
                    ),
                )
                return false
            }
            return true
        }

        private fun openCamera(context: Context) {
            if (!canStartGeneration()) return
            val activity = context as? MainActivity ?: return
            try {
                val photo = File.createTempFile("ai_capture", ".jpg")
                MainActivity.aiTempFile = photo
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.slicermore.files",
                            photo,
                        ),
                    )
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                activity.launchAppActivityResult(intent, MainActivity.REQUEST_CODE_AI_GENERATOR_TAKE_PHOTO)
            } catch (error: IOException) {
                android.util.Log.e("FileMenu", "Could not prepare a camera image for model generation", error)
                SliceBeam.EVENT_BUS.fireEvent(
                    NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.MenuFileOpenFileFailed),
                )
            }
        }

        private fun openGallery(context: Context) {
            if (!canStartGeneration()) return
            val activity = context as? MainActivity ?: return
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            activity.launchAppActivityResult(
                Intent.createChooser(intent, ""),
                MainActivity.REQUEST_CODE_AI_GENERATOR_CHOOSE_PHOTO,
            )
        }
    }
}

@Composable
private fun AIGeneratorMenuContent(
    remainingLabel: String,
    quotaProgress: Float,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AIGeneratorActionRow(
                icon = R.drawable.camera_outline_28,
                title = stringResource(R.string.MenuFileAIGeneratorFromCamera),
                onClick = onCamera,
            )
            AIGeneratorActionRow(
                icon = R.drawable.picture_outline_28,
                title = stringResource(R.string.MenuFileAIGeneratorFromGallery),
                onClick = onGallery,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = remainingLabel,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { quotaProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.arrow_left_outline_28),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.MenuOrientationPositionBack),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AIGeneratorActionRow(icon: Int, title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
