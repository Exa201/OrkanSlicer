package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.LocalBeamThemeRevision
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.SegmentsView
import java.text.DecimalFormat

/** Post-slice G-code information panel and visibility controls. */
class SliceInfoMenu(private val parentMenu: SliceMenu) : UnfoldMenu() {
    private var state by mutableStateOf(SliceInfoUiState())
    private var invalidateGl: Runnable? = null
    private var visible = false
    private var timelineVisible by mutableStateOf(false)

    private fun getViewer(): GCodeViewer? = fragment?.glView?.renderer?.viewer

    private fun getResult(): GCodeProcessorResult? = fragment?.glView?.renderer?.gcodeResult

    override fun onCreateView(ctx: Context, portrait: Boolean): View {
        val schemeNames = ctx.resources.getStringArray(R.array.GCodeColorSchemes).toList()
        return ThemeAwareComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme(themeRevision = themeRevision) {
                    SliceInfoContent(
                        state = state,
                        schemeNames = schemeNames,
                        timelineVisible = timelineVisible,
                        onSchemeSelected = ::selectScheme,
                        onSeamsChanged = ::setSeamsVisible,
                        onRoleVisibilityChanged = ::setRoleVisible,
                        onDismiss = ::dismiss,
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        visible = true
        timelineVisible = false
        refreshInfoState()
        ViewUtils.postOnMainThread(Runnable {
            if (visible) timelineVisible = true
        }, 50)
    }

    override fun onDestroy() {
        super.onDestroy()
        visible = false
        timelineVisible = false
        invalidateGl?.let { ViewUtils.removeCallbacks(it) }
        invalidateGl = null
    }

    override fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int {
        if (portrait) {
            val viewer = getViewer()
            if (viewer != null) {
                val visibleCount = (0 until GCodeViewer.EXTRUSION_ROLES_COUNT)
                    .count { viewer.getEstimatedTime(it) != 0f }
                val content = ViewUtils.dp(42) * visibleCount + ViewUtils.dp(28) + ViewUtils.dp(52) +
                    ViewUtils.dp(18 + 8) + ViewUtils.dp(44) + ViewUtils.dp(162)
                val available = into.height - into.paddingTop - into.paddingBottom - ViewUtils.dp(24)
                return minOf(content, maxOf(ViewUtils.dp(240), available))
            }
        }
        return super.getRequestedSize(into, portrait)
    }

    private fun selectScheme(index: Int) {
        val type = SCHEME_VALUES.getOrNull(index) ?: return
        val glView = fragment?.glView ?: return
        state = state.copy(schemeIndex = index)
        glView.queueEvent {
            val glViewer = getViewer() ?: return@queueEvent
            if (type == GCodeViewer.VIEW_TYPE_TOOL) {
                val rgb = Prefs.getFilamentPalette().map { it and 0xFFFFFF }.toIntArray()
                glViewer.setToolColors(rgb)
            }
            glViewer.viewType = type
            glView.requestRender()
            ViewUtils.postOnMainThread(Runnable { parentMenu.refreshInfoItems() })
        }
    }

    private fun setSeamsVisible(visible: Boolean) {
        val viewer = getViewer() ?: return
        if (viewer.isOptionVisible(GCodeViewer.OPTION_TYPE_SEAMS) != visible) {
            viewer.toggleOptionVisibility(GCodeViewer.OPTION_TYPE_SEAMS)
            fragment?.glView?.requestRender()
        }
        state = state.copy(seamsVisible = visible)
    }

    private fun setRoleVisible(role: Int, visible: Boolean) {
        val viewer = getViewer() ?: return
        val roleSummary = state.roles.firstOrNull { it.role == role } ?: return
        if (!canToggleSliceRole(state.roles.count { it.visible }, roleSummary.visible)) return
        if (viewer.isExtrusionRoleVisible(role) == visible) return

        viewer.toggleExtrusionRoleVisible(role)
        refreshInfoState()
        invalidateGl?.let { ViewUtils.removeCallbacks(it) }
        val callback = Runnable {
            invalidateGl = null
            val range = viewer.layersViewRange
            viewer.setLayersViewRange(range.first, range.second)
            fragment?.glView?.requestRender()
        }
        invalidateGl = callback
        ViewUtils.postOnMainThread(callback, 250)
    }

    private fun refreshInfoState() {
        val viewer = getViewer()
        val result = getResult()
        if (viewer == null || result == null) {
            state = SliceInfoUiState()
            return
        }

        val roleTimes = FloatArray(GCodeViewer.EXTRUSION_ROLES_COUNT) { viewer.getEstimatedTime(it) }
        val visibleRoles = BooleanArray(GCodeViewer.EXTRUSION_ROLES_COUNT) { viewer.isExtrusionRoleVisible(it) }
        val segmentValues = sliceInfoSegmentValues(roleTimes, visibleRoles)
        val roles = roleTimes.indices
            .filter { roleTimes[it] != 0f }
            .map { role ->
                SliceRoleSummary(
                    role = role,
                    title = roleTitle(role),
                    details = formatComplex(
                        result.getUsedFilamentG(role).toDouble(),
                        result.getUsedFilamentMM(role).toDouble(),
                        roleTimes[role],
                    ),
                    visible = visibleRoles[role],
                    color = SegmentsView.mapColor(role),
                )
            }

        var totalWeight = 0.0
        var totalLength = 0.0
        for (index in 0 until GCodeViewer.EXTRUSION_ROLES_COUNT) {
            totalLength += result.getUsedFilamentMM(index)
            totalWeight += result.getUsedFilamentG(index)
        }

        val config = SliceBeam.requireConfig()
        val filament = config.findFilament(config.presets?.get("filament"))
        val costPerKg = filament?.get("filament_cost")
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.toDoubleOrNull()
            ?: 0.0
        val time = viewer.getEstimatedTime()
        val layers = viewer.getLayersCount()
        val app = SliceBeam.requireInstance()
        val costSummary = if (costPerKg > 0.0) {
            summaryString(R.string.SliceSummaryCostValue, R.string.SliceSummaryCost, decimalFormat.format(totalWeight / 1000.0 * costPerKg))
        } else {
            app.getString(R.string.SliceSummaryUnavailableValue, app.getString(R.string.SliceSummaryCost))
        }

        state = SliceInfoUiState(
            schemeIndex = schemeIndex(viewer.viewType),
            seamsVisible = viewer.isOptionVisible(GCodeViewer.OPTION_TYPE_SEAMS),
            timeSummary = summaryString(R.string.SliceSummaryTimeValue, R.string.SliceSummaryTime, formatTime(time)),
            weightSummary = summaryString(R.string.SliceSummaryWeightValue, R.string.SliceSummaryWeight, decimalFormat.format(totalWeight)),
            lengthSummary = summaryString(R.string.SliceSummaryLengthValue, R.string.SliceSummaryLength, decimalFormat.format(totalLength / 1000.0)),
            layersSummary = app.getString(
                R.string.SliceSummaryLayersValue,
                app.getString(R.string.SliceSummaryLayers),
                layers,
            ),
            costSummary = costSummary,
            totalSummary = formatComplex(totalWeight, totalLength, totalEstimatedTime(viewer)),
            roles = roles,
            segmentValues = segmentValues,
        )
    }

    private fun totalEstimatedTime(viewer: GCodeViewer): Float =
        (0 until GCodeViewer.EXTRUSION_ROLES_COUNT)
            .filter { viewer.isExtrusionRoleVisible(it) }
            .sumOf { viewer.getEstimatedTime(it).toDouble() }
            .toFloat()

    private fun summaryString(valueRes: Int, labelRes: Int, value: String): String {
        val app = SliceBeam.requireInstance()
        return app.getString(valueRes, app.getString(labelRes), value)
    }

    private fun formatComplex(weight: Double, length: Double, time: Float): String = buildString {
        if (weight > 0.0) {
            append(decimalFormat.format(weight))
                .append(' ')
                .append(SliceBeam.requireInstance().getString(R.string.MenuSliceInfoWeight))
                .append(" | ")
        }
        append(decimalFormat.format(length))
            .append(' ')
            .append(SliceBeam.requireInstance().getString(R.string.MenuSliceInfoLength))
            .append(" | ")
            .append(formatTime(time))
    }

    private fun formatTime(time: Float): String = SliceInfoFormatter.formatTime(
        time,
        SliceBeam.requireInstance().getString(R.string.MenuSliceInfoHour),
        SliceBeam.requireInstance().getString(R.string.MenuSliceInfoMinute),
        SliceBeam.requireInstance().getString(R.string.MenuSliceInfoSecond),
    )

    private fun roleTitle(role: Int): String = when (role) {
        GCodeViewer.EXTRUSION_ROLE_NONE -> Slic3rLocalization.getString("Unknown")
        GCodeViewer.EXTRUSION_ROLE_PERIMETER -> Slic3rLocalization.getString("Perimeter")
        GCodeViewer.EXTRUSION_ROLE_EXTERNAL_PERIMETER -> Slic3rLocalization.getString("External perimeter")
        GCodeViewer.EXTRUSION_ROLE_OVERHANG_PERIMETER -> Slic3rLocalization.getString("Overhang perimeter")
        GCodeViewer.EXTRUSION_ROLE_INTERNAL_INFILL -> Slic3rLocalization.getString("Internal infill")
        GCodeViewer.EXTRUSION_ROLE_SOLID_INFILL -> Slic3rLocalization.getString("Solid infill")
        GCodeViewer.EXTRUSION_ROLE_TOP_SOLID_INFILL -> Slic3rLocalization.getString("Top solid infill")
        GCodeViewer.EXTRUSION_ROLE_IRONING -> Slic3rLocalization.getString("Ironing")
        GCodeViewer.EXTRUSION_ROLE_BRIDGE_INFILL -> Slic3rLocalization.getString("Bridge infill")
        GCodeViewer.EXTRUSION_ROLE_GAP_FILL -> Slic3rLocalization.getString("Gap fill")
        GCodeViewer.EXTRUSION_ROLE_SKIRT -> Slic3rLocalization.getString("Skirt/Brim")
        GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL -> Slic3rLocalization.getString("Support material")
        GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL_INTERFACE -> Slic3rLocalization.getString("Support material interface")
        GCodeViewer.EXTRUSION_ROLE_WIPE_TOWER -> Slic3rLocalization.getString("Wipe tower")
        GCodeViewer.EXTRUSION_ROLE_CUSTOM -> Slic3rLocalization.getString("Custom")
        else -> Slic3rLocalization.getString("Unknown")
    }

    private fun schemeIndex(type: Int): Int = SCHEME_VALUES.indexOf(type).takeIf { it >= 0 } ?: 0

    private companion object {
        val SCHEME_VALUES = intArrayOf(
            GCodeViewer.VIEW_TYPE_FEATURE,
            1, // Height
            2, // Width
            3, // Speed
            4, // Actual speed
            5, // Fan speed
            6, // Temperature
            7, // Volumetric flow rate
            8, // Actual volumetric flow rate
            9, // Layer time, linear
            10, // Layer time, logarithmic
            GCodeViewer.VIEW_TYPE_TOOL,
        )
        val decimalFormat = DecimalFormat("0.##")
    }
}

@Composable
private fun SliceInfoTimeline(
    segmentValues: FloatArray,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val themeRevision = LocalBeamThemeRevision.current
    val roleColors = remember(themeRevision) {
        List(GCodeViewer.EXTRUSION_ROLES_COUNT + 1) { role -> Color(SegmentsView.mapColor(role)) }
    }
    val revealProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = 1000f, dampingRatio = 1f),
        label = "timelineReveal",
    )
    val animatedBoundaries = segmentValues.mapIndexed { index, value ->
        animateFloatAsState(
            targetValue = value.coerceIn(0f, 1f),
            animationSpec = spring(stiffness = 1000f, dampingRatio = 1f),
            label = "timelineBoundary$index",
        ).value
    }

    Canvas(modifier = modifier) {
        val visibleWidth = size.width * revealProgress
        if (visibleWidth <= 0f || size.height <= 0f || animatedBoundaries.size < 2) return@Canvas

        val clip = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = visibleWidth,
                    bottom = size.height,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                ),
            )
        }
        clipPath(clip) {
            for (index in 1 until animatedBoundaries.size) {
                val start = animatedBoundaries[index - 1].coerceIn(0f, 1f)
                val end = animatedBoundaries[index].coerceIn(start, 1f)
                if (end <= start) continue
                drawRect(
                    color = roleColors.getOrElse(index - 1) { Color.Transparent },
                    topLeft = Offset(start * visibleWidth, 0f),
                    size = Size((end - start) * visibleWidth, size.height),
                )
            }
        }
    }
}

private data class SliceInfoUiState(
    val schemeIndex: Int = 0,
    val seamsVisible: Boolean = false,
    val timeSummary: String = "",
    val weightSummary: String = "",
    val lengthSummary: String = "",
    val layersSummary: String = "",
    val costSummary: String = "",
    val totalSummary: String = "",
    val roles: List<SliceRoleSummary> = emptyList(),
    val segmentValues: FloatArray = floatArrayOf(0f, 1f),
)

private data class SliceRoleSummary(
    val role: Int,
    val title: String,
    val details: String,
    val visible: Boolean,
    val color: Int,
)

internal fun canToggleSliceRole(visibleRoleCount: Int, currentlyVisible: Boolean): Boolean =
    !currentlyVisible || visibleRoleCount > 1

@Composable
private fun SliceInfoContent(
    state: SliceInfoUiState,
    schemeNames: List<String>,
    timelineVisible: Boolean,
    onSchemeSelected: (Int) -> Unit,
    onSeamsChanged: (Boolean) -> Unit,
    onRoleVisibilityChanged: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var showSchemeChooser by remember { mutableStateOf(false) }
    val selectedScheme = state.schemeIndex.coerceIn(0, (schemeNames.size - 1).coerceAtLeast(0))

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable { showSchemeChooser = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.GCodeColorScheme),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
            )
            Text(
                text = schemeNames.getOrNull(selectedScheme).orEmpty(),
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(
                    value = state.seamsVisible,
                    role = Role.Checkbox,
                    onValueChange = onSeamsChanged,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.GCodeShowSeams),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
            )
            Checkbox(checked = state.seamsVisible, onCheckedChange = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SliceSummaryCell(state.timeSummary, Modifier.weight(1f))
                SliceSummaryCell(state.weightSummary, Modifier.weight(1f))
                SliceSummaryCell(state.layersSummary, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                SliceSummaryCell(state.lengthSummary, Modifier.weight(1f))
                SliceSummaryCell(state.costSummary, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))

        val visibleRoleCount = state.roles.count { it.visible }
        state.roles.forEach { role ->
            SliceRoleRow(
                role = role,
                enabled = canToggleSliceRole(visibleRoleCount, role.visible),
                onCheckedChange = { onRoleVisibilityChanged(role.role, it) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = state.totalSummary,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        SliceInfoTimeline(
            segmentValues = state.segmentValues,
            visible = timelineVisible,
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.arrow_left_outline_28),
                contentDescription = stringResource(R.string.MenuOrientationPositionBack),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.MenuOrientationPositionBack),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    if (showSchemeChooser) {
        AlertDialog(
            onDismissRequest = { showSchemeChooser = false },
            title = { Text(stringResource(R.string.GCodeColorScheme)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    itemsIndexed(schemeNames) { index, name ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onSchemeSelected(index)
                                    showSchemeChooser = false
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == selectedScheme, onClick = {
                                onSchemeSelected(index)
                                showSchemeChooser = false
                            })
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSchemeChooser = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SliceSummaryCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.heightIn(min = 54.dp).padding(horizontal = 4.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SliceRoleRow(
    role: SliceRoleSummary,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(
                value = role.visible,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .heightIn(min = 42.dp)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = role.visible,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = Color(role.color)),
        )
        Text(
            text = role.title,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp),
            color = if (enabled || !role.visible) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = role.details,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
