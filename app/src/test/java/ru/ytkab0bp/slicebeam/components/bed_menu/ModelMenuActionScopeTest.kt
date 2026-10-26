package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.R

class ModelMenuActionScopeTest {
    @Test fun selectedObjectPlacementAndEditActionsStayInTheObjectGroup() {
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
        ).forEach { assertTrue("$it should affect one object", ModelMenuActionScope.isSingleObject(it)) }
    }

    @Test fun plateWideActionsStayInTheAllObjectsGroup() {
        listOf(
            R.string.ModelMenuArrange,
            R.string.ModelMenuOrientAll,
            R.string.ModelMenuSelectAll,
            R.string.ModelMenuDeleteAll,
        ).forEach { assertFalse("$it should affect the full plate", ModelMenuActionScope.isSingleObject(it)) }
        assertFalse("the legacy edit-menu split action should no longer be exposed", ModelMenuActionScope.isSingleObject(R.string.MenuToolbarSplit))
    }
}
