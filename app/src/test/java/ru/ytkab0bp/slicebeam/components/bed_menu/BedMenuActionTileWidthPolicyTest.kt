package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedMenuActionTileWidthPolicyTest {
    @Test
    fun narrowPhonesAndLargeTextGetFewerWiderColumns() {
        assertEquals(3, BedMenuActionTileWidthPolicy.columnsForLayout(1f, 366))
        assertEquals(4, BedMenuActionTileWidthPolicy.columnsForLayout(1f, 411))
        assertEquals(3, BedMenuActionTileWidthPolicy.columnsForLayout(1.15f, 411))
    }

    @Test
    fun fourReadableActionsFitOnAnOrdinaryPhone() {
        for (width in listOf(360, 365, 411)) {
            val tile = BedMenuActionTileWidthPolicy.widthDp(width, 10)
            assertTrue(tile * 4 + 8 <= width)
            assertTrue(tile >= 72)
        }
    }

    @Test
    fun narrowPhonesKeepReadableActionTargets() {
        assertTrue(BedMenuActionTileWidthPolicy.widthDp(320, 10) >= 72)
    }

    @Test
    fun shortMenusKeepAllActionsVisibleWithRoomForTheirLabels() {
        val tile = BedMenuActionTileWidthPolicy.widthDp(411, 3)
        assertTrue(tile * 3 + 8 <= 411)
        assertTrue(tile >= 100)
    }

    @Test
    fun tabletTilesStayCompact() {
        assertTrue(BedMenuActionTileWidthPolicy.widthDp(840, 5) <= 128)
    }

    @Test
    fun actionMenuUsesSecondRowWhenActionsWouldBeClipped() {
        assertFalse(BedMenuActionTileWidthPolicy.usesTwoRows(3, true, 411, 1f))
        assertTrue(BedMenuActionTileWidthPolicy.usesTwoRows(5, true, 411, 1f))
        assertTrue(BedMenuActionTileWidthPolicy.usesTwoRows(4, true, 320, 1f))
        assertTrue(BedMenuActionTileWidthPolicy.usesTwoRows(4, true, 411, 1.5f))
        assertFalse(BedMenuActionTileWidthPolicy.usesTwoRows(8, false, 411, 1.5f))
    }
}
