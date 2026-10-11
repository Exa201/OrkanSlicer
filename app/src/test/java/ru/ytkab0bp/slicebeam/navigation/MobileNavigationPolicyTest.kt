package ru.ytkab0bp.slicebeam.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileNavigationPolicyTest {
    @Test
    fun mainDestinationsFollowTheSliceBeamWorkspaceOrder() {
        assertEquals(
            listOf(
                MobileNavigationPolicy.PREVIEW_SLOT,
                MobileNavigationPolicy.PRINT_SLOT,
                MobileNavigationPolicy.FILAMENT_SLOT,
                MobileNavigationPolicy.PRINTER_SLOT,
                MobileNavigationPolicy.APP_SLOT,
            ),
            MobileNavigationPolicy.destinations(),
        )
    }

    @Test
    fun makerRemainsAvailableAsAnAppSubsectionInsteadOfTakingAMainTab() {
        assertFalse(MobileNavigationPolicy.isDestination(MobileNavigationPolicy.MAKER_SLOT))
        assertTrue(MobileNavigationPolicy.isDestination(MobileNavigationPolicy.APP_SLOT))
        assertEquals(
            MobileNavigationPolicy.APP_SLOT,
            MobileNavigationPolicy.selectedDestination(MobileNavigationPolicy.MAKER_SLOT),
        )
    }
}
