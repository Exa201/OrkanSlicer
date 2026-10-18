package ru.ytkab0bp.slicebeam.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.BeamServerData
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.launchAppActivityResult
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SetupActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.ColorChooserDialog
import ru.ytkab0bp.slicebeam.events.BeamServerDataUpdatedEvent
import ru.ytkab0bp.slicebeam.events.CloudUserInfoUpdatedEvent
import ru.ytkab0bp.slicebeam.navigation.Fragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.theme.BeamTheme
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.SettingsMenuLayoutPolicy
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceMessageDialog
import ru.ytkab0bp.slicebeam.utils.ConverterStorage
import ru.ytkab0bp.slicebeam.utils.Prefs

/** App settings rendered in Compose, separate from the profile editing screens. */
class SettingsFragment : Fragment() {
    private var themeRevision by mutableIntStateOf(0)
    private var expandedSections by mutableStateOf(emptySet<Int>())
    private var activeChoiceDialog by mutableStateOf<SettingsChoiceDialogState?>(null)
    private var showAccentColorDialog by mutableStateOf(false)
    private var showFolderPickerError by mutableStateOf(false)
    private var showResetConfirmation by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        SliceBeam.EVENT_BUS.registerListener(this)
    }

    override fun onDestroy() {
        activeChoiceDialog = null
        showAccentColorDialog = false
        showFolderPickerError = false
        showResetConfirmation = false
        SliceBeam.EVENT_BUS.unregisterListener(this)
        super.onDestroy()
    }

    override fun onApplyTheme() {
        super.onApplyTheme()
        themeRevision++
    }

    override fun onCreateView(ctx: Context): View = ComposeView(ctx).apply {
        setContent {
            val revision = themeRevision
            if (revision >= 0) {
                BeamComposeTheme(themeRevision = revision) {
                    Box(Modifier.fillMaxSize()) {
                        SettingsContent(
                            expandedSections = expandedSections,
                            cloudAvailable = BeamServerData.isCloudAvailable(),
                            boostyAvailable = BeamServerData.isBoostyAvailable(),
                            themeName = ctx.getString(Prefs.getThemeMode().title),
                            accentColor = "#%08X".format(Prefs.getAccentColor()),
                            renderScale = "${(Prefs.getRenderScale() * 100).toInt()}%",
                            uiTextScale = "${(Prefs.getUiTextScale() * 100).toInt()}%",
                            converterRoot = ConverterStorage.getRootDisplayName(ctx),
                            onToggleSection = ::toggleSection,
                            onMaker = ::openMaker,
                            onTheme = ::showThemePicker,
                            onAccent = ::showAccentPicker,
                            onRenderScale = ::showRenderScalePicker,
                            onUiTextScale = ::showUiTextScalePicker,
                            onConverterFolder = ::chooseConverterFolder,
                            onCloud = { startSetup(SetupActivity.EXTRA_CLOUD_PROFILE) },
                            onAddPrinter = { startSetup(SetupActivity.EXTRA_ADD_PRINTER) },
                            onAbout = { startSetup(SetupActivity.EXTRA_ABOUT) },
                            onBoosty = { startSetup(SetupActivity.EXTRA_BOOSTY_ONLY) },
                            onReset = ::confirmReset,
                        )
                        activeChoiceDialog?.let { dialog ->
                            SettingsChoiceDialog(dialog) { activeChoiceDialog = null }
                        }
                        if (showAccentColorDialog) {
                            ColorChooserDialog(
                                initialColor = Prefs.getAccentColor(),
                                resetLabel = ctx.getString(R.string.SettingsInterfaceColorReset),
                                onDismiss = { showAccentColorDialog = false },
                                onColorChosen = { color ->
                                    Prefs.setAccentColor(color)
                                    applyAccentColor()
                                },
                                onReset = {
                                    Prefs.setAccentColor(SetupActivity.AccentColors.DEFAULT.color)
                                    applyAccentColor()
                                },
                            )
                        }
                        if (showFolderPickerError) {
                            SettingsFolderPickerErrorDialog(
                                onDismiss = { showFolderPickerError = false },
                            )
                        }
                        if (showResetConfirmation) {
                            SettingsResetConfirmationDialog(
                                onDismiss = { showResetConfirmation = false },
                                onConfirm = {
                                    showResetConfirmation = false
                                    performReset()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toggleSection(index: Int) {
        expandedSections = if (index in expandedSections) expandedSections - index else expandedSections + index
    }

    private fun openMaker() {
        val activity = getContext() as? MainActivity ?: return
        val navigation = activity.navigationDelegate as? MobileNavigationDelegate ?: return
        navigation.pushFragment(MakerFragment())
    }

    private fun startSetup(extra: String) {
        val activity = getContext() as? Activity ?: return
        activity.startActivity(Intent(activity, SetupActivity::class.java).putExtra(extra, true))
    }

    private fun showThemePicker() {
        val context = getContext() ?: return
        val modes = Prefs.ThemeMode.values()
        showChoiceDialog(
            title = R.string.SettingsInterfaceTheme,
            options = modes.map { context.getString(it.title) },
            selectedIndex = Prefs.getThemeMode().ordinal,
        ) { which ->
            Prefs.setThemeMode(which)
            (getContext() as? Activity)?.let { ThemesRepo.invalidate(it) }
            themeRevision++
        }
    }

    private fun showChoiceDialog(
        title: Int,
        options: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit,
    ) {
        activeChoiceDialog = SettingsChoiceDialogState(title, options, selectedIndex) { index ->
            if (index in options.indices) {
                onSelected(index)
                activeChoiceDialog = null
            }
        }
    }

    private fun showAccentPicker() {
        if (getContext() != null) showAccentColorDialog = true
    }

    private fun applyAccentColor() {
        val color = Prefs.getAccentColor()
        BeamTheme.LIGHT.colors.put(android.R.attr.colorAccent, color)
        BeamTheme.DARK.colors.put(android.R.attr.colorAccent, color)
        (getContext() as? Activity)?.let { ThemesRepo.invalidate(it) }
        themeRevision++
    }

    private fun showRenderScalePicker() {
        if (getContext() == null) return
        val variants = floatArrayOf(1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f)
        val labels = variants.map { "${(it * 100).toInt()}%" }
        val selected = variants.indexOfFirst { it == Prefs.getRenderScale() }.coerceAtLeast(0)
        showChoiceDialog(R.string.SettingsInterfaceResolutionScale, labels, selected) { which ->
            Prefs.setRenderScale(variants[which])
            themeRevision++
        }
    }

    private fun showUiTextScalePicker() {
        val context = getContext() ?: return
        val variants = floatArrayOf(0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f)
        val labels = variants.map { scale ->
            val sizeLabel = when {
                scale < 1f -> context.getString(R.string.SettingsTextSizeCompact)
                scale == 1f -> context.getString(R.string.SettingsTextSizeStandard)
                scale < 1.3f -> context.getString(R.string.SettingsTextSizeLarge)
                else -> context.getString(R.string.SettingsTextSizeExtraLarge)
            }
            "$sizeLabel · ${(scale * 100).toInt()}%"
        }
        val selected = variants.indexOfFirst { it == Prefs.getUiTextScale() }.coerceAtLeast(0)
        showChoiceDialog(R.string.SettingsInterfaceTextSize, labels, selected) { which ->
            Prefs.setUiTextScale(variants[which])
            (getContext() as? Activity)?.let { ThemesRepo.invalidate(it) } ?: run { themeRevision++ }
        }
    }

    private fun chooseConverterFolder() {
        val activity = getContext() as? Activity ?: return
        try {
            activity.launchAppActivityResult(
                ConverterStorage.createFolderPickerIntent(),
                MainActivity.REQUEST_CODE_CONVERTER_ROOT_SETTINGS,
            )
        } catch (_: Exception) {
            showFolderPickerError = true
        }
    }

    private fun confirmReset() {
        showResetConfirmation = true
    }

    private fun performReset() {
        val activity = getContext() as? Activity ?: return
        SliceBeam.getConfigFile().delete()
        SliceBeam.CONFIG = null
        Prefs.getPrefs()?.edit()?.clear()?.apply()
        Prefs.setLastCommit()
        activity.startActivity(Intent(activity, SetupActivity::class.java))
        activity.finish()
    }

    fun refreshConverterFolderSetting() {
        themeRevision++
    }

    @EventHandler(runOnMainThread = true)
    fun onDataUpdated(event: BeamServerDataUpdatedEvent) {
        themeRevision++
    }

    @EventHandler(runOnMainThread = true)
    fun onUserInfoUpdated(event: CloudUserInfoUpdatedEvent) {
        if (BeamServerData.isCloudAvailable()) themeRevision++
    }
}

@Composable
internal fun SettingsFolderPickerErrorDialog(onDismiss: () -> Unit) {
    WorkspaceMessageDialog(
        title = stringResource(R.string.ConverterFailed),
        message = stringResource(R.string.ConverterFolderPickerUnavailable),
        confirmLabel = stringResource(android.R.string.ok),
        dismissLabel = null,
        onDismiss = onDismiss,
        onConfirm = onDismiss,
    )
}

@Composable
internal fun SettingsResetConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    WorkspaceMessageDialog(
        title = stringResource(R.string.SettingsResetToDefaultTitle),
        message = stringResource(R.string.SettingsResetToDefaultDescription),
        confirmLabel = stringResource(android.R.string.ok),
        dismissLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun SettingsContent(
    expandedSections: Set<Int>,
    cloudAvailable: Boolean,
    boostyAvailable: Boolean,
    themeName: String,
    accentColor: String,
    renderScale: String,
    uiTextScale: String,
    converterRoot: String,
    onToggleSection: (Int) -> Unit,
    onMaker: () -> Unit,
    onTheme: () -> Unit,
    onAccent: () -> Unit,
    onRenderScale: () -> Unit,
    onUiTextScale: () -> Unit,
    onConverterFolder: () -> Unit,
    onCloud: () -> Unit,
    onAddPrinter: () -> Unit,
    onAbout: () -> Unit,
    onBoosty: () -> Unit,
    onReset: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val compactLayout = SettingsMenuLayoutPolicy.usesCompactLayout(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    )
    val shortLandscapeLayout = SettingsMenuLayoutPolicy.usesTwoColumnLayout(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    )
    val screenHeight = configuration.screenHeightDp.dp
    Column(
        modifier = Modifier
            .heightIn(max = screenHeight)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = if (compactLayout) 14.dp else 18.dp,
                vertical = if (compactLayout) 8.dp else 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
            Text(
                text = stringResource(R.string.SlotAppSettings),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = if (compactLayout) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (!shortLandscapeLayout) {
                Text(
                    text = stringResource(R.string.SettingsScreenDescription),
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 2.dp,
                        bottom = if (compactLayout) 10.dp else 18.dp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (compactLayout) 13.sp else 15.sp,
                    lineHeight = if (compactLayout) 17.sp else 21.sp,
                    maxLines = if (compactLayout) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            SettingsActionRow(
                R.drawable.magic_wand_outline_28,
                R.string.SettingsAppMakerTitle,
                if (shortLandscapeLayout) null else stringResource(R.string.SettingsAppMakerDescription),
                onMaker,
                modifier = Modifier.padding(bottom = if (compactLayout) 8.dp else 12.dp),
                compactLayout = compactLayout,
                denseLandscape = shortLandscapeLayout,
            )

            val cloudSection: @Composable () -> Unit = {
                if (cloudAvailable) {
                    SettingsSection(
                        index = 4,
                        expandedSections = expandedSections,
                        onToggle = onToggleSection,
                        iconRes = R.drawable.cloud_plus_outline_28,
                        title = stringResource(R.string.SettingsCloudSection),
                        compactLayout = compactLayout,
                        denseLandscape = shortLandscapeLayout,
                    ) {
                        SettingsActionRow(
                            R.drawable.cloud_plus_outline_28,
                            R.string.SettingsCloudProfile,
                            stringResource(R.string.SettingsCloudProfileDescription),
                            onCloud,
                            compactLayout = compactLayout,
                        )
                    }
                }
            }

            val interfaceSection: @Composable () -> Unit = {
                SettingsSection(
                    index = 0,
                    expandedSections = expandedSections,
                    onToggle = onToggleSection,
                    iconRes = R.drawable.paint_roller_outline_28,
                    title = stringResource(R.string.SettingsInterface),
                    compactLayout = compactLayout,
                    denseLandscape = shortLandscapeLayout,
                ) {
                    SettingsRow(
                        R.string.SettingsInterfaceTheme,
                        value = themeName,
                        onClick = onTheme,
                        compactLayout = compactLayout,
                    )
                    SettingsRow(
                        R.string.SettingsInterfaceColor,
                        value = accentColor,
                        onClick = onAccent,
                        compactLayout = compactLayout,
                    )
                    SettingsRow(
                        R.string.SettingsInterfaceTextSize,
                        description = stringResource(R.string.SettingsInterfaceTextSizeDescription),
                        value = uiTextScale,
                        onClick = onUiTextScale,
                        compactLayout = compactLayout,
                    )
                    SettingsRow(
                        R.string.SettingsInterfaceResolutionScale,
                        description = stringResource(R.string.SettingsInterfaceResolutionScaleDescription),
                        value = renderScale,
                        onClick = onRenderScale,
                        compactLayout = compactLayout,
                    )
                }
            }

            val converterSection: @Composable () -> Unit = {
                SettingsSection(
                    index = 1,
                    expandedSections = expandedSections,
                    onToggle = onToggleSection,
                    iconRes = R.drawable.picture_outline_28,
                    title = stringResource(R.string.ConverterFolderSection),
                    compactLayout = compactLayout,
                    denseLandscape = shortLandscapeLayout,
                ) {
                    SettingsRow(
                        R.string.ConverterFolderSetting,
                        description = stringResource(R.string.ConverterFolderSettingDescription),
                        value = converterRoot,
                        onClick = onConverterFolder,
                        compactLayout = compactLayout,
                    )
                }
            }

            val devicesSection: @Composable () -> Unit = {
                SettingsSection(
                    index = 2,
                    expandedSections = expandedSections,
                    onToggle = onToggleSection,
                    iconRes = R.drawable.printer_outline_28,
                    title = stringResource(R.string.SettingsDevices),
                    compactLayout = compactLayout,
                    denseLandscape = shortLandscapeLayout,
                ) {
                    SettingsActionRow(
                        R.drawable.printer_outline_28,
                        R.string.SettingsAddPrinter,
                        stringResource(R.string.SettingsAddPrinterDescription),
                        onAddPrinter,
                        compactLayout = compactLayout,
                    )
                }
            }

            val informationSection: @Composable () -> Unit = {
                SettingsSection(
                    index = 3,
                    expandedSections = expandedSections,
                    onToggle = onToggleSection,
                    iconRes = R.drawable.info_outline_28,
                    title = stringResource(R.string.SettingsAppInformation),
                    compactLayout = compactLayout,
                    denseLandscape = shortLandscapeLayout,
                ) {
                    SettingsActionRow(
                        R.drawable.info_outline_28,
                        R.string.SettingsAbout,
                        stringResource(R.string.SettingsAboutDescription),
                        onAbout,
                        compactLayout = compactLayout,
                    )
                    if (boostyAvailable) {
                        SettingsActionRow(
                            R.drawable.boosty,
                            R.string.SettingsBoosty,
                            null,
                            onBoosty,
                            compactLayout = compactLayout,
                        )
                    }
                }
            }

            if (shortLandscapeLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        cloudSection()
                        interfaceSection()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        devicesSection()
                        converterSection()
                        informationSection()
                    }
                }
            } else {
                cloudSection()
                interfaceSection()
                devicesSection()
                converterSection()
                informationSection()
            }

            SettingsActionRow(
                R.drawable.refresh_outline_28,
                R.string.SettingsResetToDefault,
                null,
                onReset,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                destructive = true,
                compactLayout = compactLayout,
                denseLandscape = shortLandscapeLayout,
            )
        }
    }
}

@Composable
internal fun SettingsSection(
    index: Int,
    expandedSections: Set<Int>,
    onToggle: (Int) -> Unit,
    @DrawableRes iconRes: Int,
    title: String,
    compactLayout: Boolean,
    denseLandscape: Boolean = false,
    content: @Composable () -> Unit,
) {
    val expanded = index in expandedSections
    val expansionStateDescription = stringResource(if (expanded) R.string.MakerCollapse else R.string.MakerExpand)
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = if (denseLandscape) 6.dp else if (compactLayout) 8.dp else 12.dp),
        shape = RoundedCornerShape(if (compactLayout) 26.dp else 30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onToggle(index) }
                    .semantics { stateDescription = expansionStateDescription }
                    .heightIn(min = if (compactLayout) 48.dp else 56.dp)
                    .padding(
                        horizontal = if (compactLayout) 12.dp else 14.dp,
                        vertical = if (denseLandscape) 6.dp else if (compactLayout) 9.dp else 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (compactLayout) 20.dp else 24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp),
                    maxLines = if (compactLayout) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compactLayout) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    painter = painterResource(R.drawable.dropdown_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = if (compactLayout) 10.dp else 12.dp,
                        end = if (compactLayout) 10.dp else 12.dp,
                        bottom = if (compactLayout) 10.dp else 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    titleRes: Int,
    description: String? = null,
    value: String,
    onClick: () -> Unit,
    compactLayout: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compactLayout) 52.dp else 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (compactLayout) 12.dp else 14.dp,
                vertical = if (compactLayout) 9.dp else 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = stringResource(titleRes),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compactLayout) 15.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (compactLayout) 12.sp else 13.sp,
                    )
                }
                Text(
                    text = value,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = if (compactLayout) 13.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
        }
    }
}

@Composable
private fun SettingsActionRow(
    @DrawableRes iconRes: Int,
    titleRes: Int,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    compactLayout: Boolean,
    denseLandscape: Boolean = false,
) {
    val foreground = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (denseLandscape) 48.dp else if (compactLayout) 52.dp else 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (compactLayout) 12.dp else 14.dp,
                vertical = if (denseLandscape) 6.dp else if (compactLayout) 8.dp else 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(if (denseLandscape) 20.dp else if (compactLayout) 22.dp else 24.dp),
                tint = if (destructive) foreground else MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    text = stringResource(titleRes),
                    color = foreground,
                    fontSize = if (denseLandscape) 14.sp else if (compactLayout) 15.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (denseLandscape) 11.sp else if (compactLayout) 12.sp else 13.sp,
                    lineHeight = if (compactLayout) 14.sp else 16.sp,
                        maxLines = if (compactLayout) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!destructive) Text("›", color = MaterialTheme.colorScheme.primary, fontSize = if (compactLayout) 22.sp else 26.sp)
        }
    }
}
