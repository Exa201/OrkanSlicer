package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertTrue
import org.junit.Test

class BedMenuActionTileWidthPolicyTest {
    @Test fun fourReadableActionsFitOnAnOrdinaryPhone() {
        for (width in listOf(360, 365, 411)) {
            val tile = BedMenuActionTileWidthPolicy.widthDp(width, 10)
            assertTrue(tile * 4 + 8 <= width)
            assertTrue(tile >= 72)
        }
    }

    @Test fun narrowPhonesKeepReadableActionTargets() {
        assertTrue(BedMenuActionTileWidthPolicy.widthDp(320, 10) >= 72)
    }

    @Test fun shortMenusKeepAllActionsVisibleWithRoomForTheirLabels() {
        val tile = BedMenuActionTileWidthPolicy.widthDp(411, 3)
        assertTrue(tile * 3 + 8 <= 411)
        assertTrue(tile >= 100)
    }

    @Test fun tabletTilesStayCompact() {
        assertTrue(BedMenuActionTileWidthPolicy.widthDp(840, 5) <= 128)
    }
}
