package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.R

class SelectedObjectQuickActionPolicyTest {
    @Test
    fun selectionToolbarKeepsEveryActionInTheExpectedOrder() {
        assertEquals(
            listOf(
                SelectedObjectQuickAction.MOVE,
                SelectedObjectQuickAction.ROTATE,
                SelectedObjectQuickAction.SCALE,
                SelectedObjectQuickAction.DUPLICATE,
            ),
            SelectedObjectQuickActionPolicy.actions,
        )
    }

    @Test
    fun actionsKeepTheirLocalizedLabels() {
        assertEquals(
            listOf(
                R.string.ObjectActionMove,
                R.string.ObjectActionRotate,
                R.string.ObjectActionScale,
                R.string.ObjectActionDuplicate,
            ),
            SelectedObjectQuickActionPolicy.actions.map { it.labelRes },
        )
    }
}
