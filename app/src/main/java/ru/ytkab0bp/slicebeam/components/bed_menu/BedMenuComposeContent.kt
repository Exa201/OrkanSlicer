package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A menu route whose visible workspace content is hosted directly by WorkspaceShell. */
internal interface WorkspaceComposeBedMenu {
    @Composable
    fun WorkspaceContent(portrait: Boolean)
}

/** A menu action that can be shared by responsive Compose menus. */
internal data class BedComposeMenuAction(
    val titleRes: Int,
    val iconRes: Int,
    val onClick: (View) -> Unit,
    val selected: Boolean = false,
    val toggleable: Boolean = false,
    val sectionBreakBefore: Boolean = false,
    val enabled: Boolean = true,
)

/** Keeps phone portrait actions wide enough to read; longer menus remain swipeable. */
internal object BedMenuActionTileWidthPolicy {
    fun columnsForLayout(fontScale: Float, screenWidthDp: Int): Int =
        if (fontScale >= 1.15f || screenWidthDp < 400) 3 else 4

    /** Avoid showing a clipped last action in a single-row phone menu. */
    fun usesTwoRows(actionCount: Int, portrait: Boolean, screenWidthDp: Int, fontScale: Float): Boolean =
        portrait && actionCount > columnsForLayout(fontScale, screenWidthDp)

    fun widthDp(screenWidthDp: Int, actionCount: Int): Int = when {
        actionCount <= 0 -> 72
        else -> ((screenWidthDp.coerceAtLeast(320) - 8) / actionCount.coerceIn(1, 4)).coerceIn(72, 128)
    }
}

@Composable
internal fun BedMenuActionsContent(actions: List<BedComposeMenuAction>, portrait: Boolean, multiRow: Boolean = false) {
    val menuView = LocalView.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    if (portrait && (multiRow || BedMenuActionTileWidthPolicy.usesTwoRows(
            actions.size, portrait, widthDp, fontScale,
        ))) {
        val columns = BedMenuActionTileWidthPolicy.columnsForLayout(fontScale, widthDp)
        val tileWidth = BedMenuActionTileWidthPolicy.widthDp(widthDp, columns).dp
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(actions) { action ->
                BedMenuActionTile(action, portrait = true, menuView = menuView,
                    modifier = Modifier.width(tileWidth))
            }
        }
        return
    }
    if (portrait) {
        val portraitActionTileWidth = BedMenuActionTileWidthPolicy.widthDp(
            LocalConfiguration.current.screenWidthDp,
            actions.size,
        ).dp
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(actions, key = { index, _ -> index }) { _, action ->
                if (action.sectionBreakBefore) Spacer(Modifier.width(6.dp))
                BedMenuActionTile(
                    action,
                    portrait = true,
                    menuView = menuView,
                    modifier = Modifier.width(portraitActionTileWidth).height(OrkanUiTokens.menuTileHeight),
                )
            }
        }
    } else {
        Column(
            // A horizontal Android parent can temporarily measure MATCH_PARENT children
            // with an unbounded height while the navigation layout is being rebuilt.
            // Give the scroll container a finite outer cap before applying verticalScroll.
            modifier = Modifier
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            actions.forEach { action ->
                if (action.sectionBreakBefore) Spacer(Modifier.heightIn(min = 6.dp))
                BedMenuActionTile(action, portrait = false, menuView = menuView, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BedMenuActionTile(
    action: BedComposeMenuAction,
    portrait: Boolean,
    menuView: View,
    compactPortraitGrid: Boolean = false,
    modifier: Modifier,
) {
    val fullTitle = stringResource(action.titleRes)
    val fontScale = LocalDensity.current.fontScale
    val compactLabel = when (action.titleRes) {
        R.string.MenuCameraIsometric -> if (fontScale >= 1.3f) R.string.MenuCameraIsometricCompact else action.titleRes
        R.string.ModelUndo -> R.string.EditUndoShort
        R.string.ModelRedo -> R.string.EditRedoShort
        R.string.MenuArrangeAllObjects -> R.string.EditArrangeAllShort
        R.string.MenuOrientAllObjects -> R.string.EditOrientShort
        R.string.MenuOrientationArrange -> R.string.EditPackPlateShort
        R.string.MenuFileMoveToPlate -> R.string.EditMovePlateShort
        R.string.ModelContextFillBed -> R.string.EditFillBedShort
        R.string.ModelContextCenterOnBed -> R.string.EditCenterBedShort
        R.string.ModelMenuDuplicate -> R.string.ModelMenuDuplicateShort
        R.string.ModelMenuMoveToPlate -> R.string.ModelMenuMoveToPlateShort
        R.string.ModelMenuCenterOnBed -> R.string.ModelMenuCenterOnBedShort
        R.string.ModelMenuFillBed -> R.string.ModelMenuFillBedShort
        R.string.ModelMenuArrange -> R.string.ModelMenuArrangeShort
        R.string.ModelMenuOrientAll -> R.string.ModelMenuOrientAllShort
        R.string.ModelMenuAutoOrientSelected -> R.string.ModelMenuAutoOrientSelectedShort
        R.string.ModelMenuFlatten -> R.string.ModelMenuFlattenShort
        R.string.ModelMenuDeleteSelected -> R.string.ModelMenuDeleteSelectedShort
        R.string.ModelMenuSelectAll -> R.string.ModelMenuSelectAllShort
        R.string.ModelMenuDeleteAll -> R.string.ModelMenuDeleteAllShort
        R.string.WorkspaceObjects -> R.string.ModelMenuObjectsShort
        R.string.ModelContextResetRotation -> R.string.EditResetRotationShort
        R.string.ModelContextResetScale -> R.string.EditResetScaleShort
        R.string.ModelContextSetOnFace -> R.string.EditLayFaceShort
        R.string.ModelContextSelectAll -> R.string.EditSelectAllShort
        R.string.ModelContextDeleteAll -> R.string.EditDeleteAllShort
        R.string.ModelColorSlotsAction -> R.string.ModelColorSlotsShort
        R.string.MenuSlicePerformance -> R.string.GCodePerformanceShort
        R.string.MenuSliceExportToFile -> R.string.GCodeExportShort
        R.string.MenuCameraOrtho -> R.string.MenuCameraOrthoShort
        R.string.MenuOrientationAutoOrient -> R.string.MenuActionAutoOrientShort
        R.string.ModelSimplify -> R.string.MenuActionSimplifyShort
        R.string.MenuOrientationFlatten -> R.string.MenuActionLayFlatShort
        R.string.MenuToolbarSupportPaint -> R.string.MenuActionSupportShort
        R.string.MenuToolbarSeamPaint -> R.string.MenuActionSeamShort
        R.string.MenuToolbarFuzzyPaint -> R.string.MenuActionTextureShort
        R.string.MenuToolbarColorPaint -> R.string.MenuActionColorShort
        R.string.MenuToolbarVariableLayerHeight -> R.string.MenuActionLayerHeightShort
        R.string.MenuToolbarEmboss -> R.string.MenuActionEmbossShort
        R.string.MenuToolbarMeasure -> R.string.MenuActionMeasureShort
        else -> action.titleRes
    }
    val background = (if (action.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        .copy(alpha = if (action.enabled) 1f else 0.38f)
    val foreground = (if (action.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        .copy(alpha = if (action.enabled) 1f else 0.55f)
    val interaction = if (action.toggleable) {
        Modifier.toggleable(
            value = action.selected,
            enabled = action.enabled,
            role = Role.Switch,
            onValueChange = { action.onClick(menuView) },
        )
    } else {
        Modifier.clickable(enabled = action.enabled, role = Role.Button) { action.onClick(menuView) }
    }
    Surface(
        modifier = modifier.padding(3.dp).semantics { contentDescription = fullTitle }.then(interaction),
        shape = RoundedCornerShape(OrkanUiTokens.cornerRadius),
        color = background,
        contentColor = foreground,
    ) {
        if (portrait) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(action.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(foreground),
                    modifier = Modifier.size(OrkanUiTokens.menuIconSize),
                )
                Text(
                    text = stringResource(compactLabel),
                    color = foreground,
                    fontSize = OrkanUiTokens.menuLabelSize,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = OrkanUiTokens.touchTarget).padding(horizontal = 3.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(action.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(foreground),
                    modifier = Modifier.size(OrkanUiTokens.menuIconSize),
                )
                Text(
                    text = stringResource(compactLabel),
                    color = foreground,
                    fontSize = OrkanUiTokens.menuLabelSize,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            }
        }
    }
}
