package ru.ytkab0bp.slicebeam.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupCloudSubscriptionPolicyTest {
    @Test
    fun freeAndLowerTiersAreNotShownAsCheckoutActions() {
        assertFalse(SetupCloudSubscriptionPolicy.isSubscribed(level = 0, currentLevel = 2))
        assertFalse(SetupCloudSubscriptionPolicy.canOpenCheckout(level = 0, currentLevel = null))
        assertFalse(SetupCloudSubscriptionPolicy.canOpenCheckout(level = 1, currentLevel = 2))
    }

    @Test
    fun currentTierOpensManagementAndHigherTierCanOpenCheckout() {
        assertTrue(SetupCloudSubscriptionPolicy.isSubscribed(level = 2, currentLevel = 2))
        assertTrue(SetupCloudSubscriptionPolicy.canOpenCheckout(level = 3, currentLevel = 2))
        assertTrue(SetupCloudSubscriptionPolicy.canOpenCheckout(level = 1, currentLevel = null))
    }

    @Test
    fun benefitsStartAtTheirConfiguredTierAndCanBeDisabled() {
        assertFalse(SetupCloudSubscriptionPolicy.includes(requiredLevel = -1, level = 10))
        assertFalse(SetupCloudSubscriptionPolicy.includes(requiredLevel = 3, level = 2))
        assertTrue(SetupCloudSubscriptionPolicy.includes(requiredLevel = 3, level = 3))
        assertTrue(SetupCloudSubscriptionPolicy.includes(requiredLevel = 3, level = 4))
    }
}
