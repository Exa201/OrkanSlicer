package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.R

class ModelMenuActionScopeTest {
    @Test fun selectedObjectPlacementAndEditActionsStayWithTheSelection() {
        listOf(
            R.string.WorkspaceObjects,
            R.string.ModelMenuDuplicate,
            R.string.ModelMenuMoveToPlate,
            R.string.ModelMenuCenterOnBed,
            R.string.ModelMenuFillBed,
            R.string.ModelMenuAutoOrientSelected,
            R.string.ModelMenuFlatten,
            R.string.ModelMenuDeleteSelected,
            R.string.ModelContextResetRotation,
            R.string.ModelContextResetScale,
            R.string.MenuToolbarMove,
            R.string.MenuToolbarRotate,
            R.string.MenuTransformScale,
            R.string.ModelSimplify,
            R.string.MenuTransformCut,
            R.string.ModelMenuSplit,
            R.string.MenuToolbarVariableLayerHeight,
            R.string.MenuToolbarSupportPaint,
            R.string.MenuToolbarSeamPaint,
            R.string.MenuToolbarFuzzyPaint,
            R.string.MenuToolbarColorPaint,
            R.string.MenuToolbarEmboss,
        ).forEach { assertTrue("$it should be available in the selection group", ModelMenuActionScope.isSelectionScoped(it)) }
    }

    @Test fun selectionAndPlateWideActionsAreKeptSeparate() {
        listOf(
            R.string.ModelMenuArrange,
            R.string.ModelMenuOrientAll,
            R.string.ModelMenuSelectAll,
            R.string.ModelMenuDeleteAll,
        ).forEach { assertFalse("$it should affect the full plate", ModelMenuActionScope.isSelectionScoped(it)) }
        assertFalse("the legacy edit-menu split action should no longer be exposed", ModelMenuActionScope.isSelectionScoped(R.string.MenuToolbarSplit))
    }

    @Test fun selectedGroupActionsStayWithSelectionScopedActions() {
        listOf(
            R.string.ModelMenuPlaceGroupOnBed,
            R.string.ModelMenuPlaceOnObject,
            R.string.ModelMenuPlaceOnSurface,
            R.string.ModelMenuAlignSelected,
            R.string.ModelMenuDistributeSelected,
            R.string.ModelMenuCombineSelected,
            R.string.ModelMenuBoolean,
        ).forEach { assertTrue("$it operates on the current selection", ModelMenuActionScope.isSelectionScoped(it)) }
    }
}
