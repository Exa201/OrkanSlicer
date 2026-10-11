package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.cloud.CloudAPI

class CloudManageScreenStateTest {
    @Test
    fun selectsHighestEligibleSubscriptionAndChecksSyncEntitlement() {
        val info = userInfo(level = 2, name = "Galal")
        val features = features(
            2,
            subscription(-1, "https://ignored.invalid"),
            subscription(1, "https://basic.example/manage"),
            subscription(2, "https://plus.example/manage"),
            subscription(3, "https://future.example/manage"),
        )

        val state = CloudManageScreenState.resolve(info, features, syncEnabled = false)

        assertEquals("Galal", state.accountName)
        assertFalse(state.isLoading)
        assertTrue(state.hasSubscription)
        assertEquals("https://plus.example/manage", state.subscriptionUrl)
        assertTrue(state.canSyncProfiles)
        assertFalse(state.syncEnabled)
    }

    @Test
    fun hidesSyncAndSubscriptionWhenAccountHasNoEligibleEntitlements() {
        val state = CloudManageScreenState.resolve(
            userInfo(level = 0, name = "Local-first user"),
            features(
                0,
                subscription(-1, "https://ignored.invalid"),
                subscription(1, "https://future.example/manage"),
            ),
            syncEnabled = true,
        )

        assertFalse(state.isLoading)
        assertFalse(state.hasSubscription)
        assertNull(state.subscriptionUrl)
        assertFalse(state.canSyncProfiles)
        assertTrue(state.syncEnabled)
    }

    @Test
    fun missingCloudDataShowsLoadingInsteadOfThrowing() {
        val state = CloudManageScreenState.resolve(
            userInfo(level = 1, name = "Waiting"),
            features = null,
            syncEnabled = true,
        )

        assertTrue(state.isLoading)
        assertEquals("Waiting", state.accountName)
        assertFalse(state.hasSubscription)
        assertNull(state.subscriptionUrl)
        assertFalse(state.canSyncProfiles)
    }

    private fun userInfo(level: Int, name: String): CloudAPI.UserInfo = CloudAPI.UserInfo().apply {
        currentLevel = level
        displayName = name
    }

    private fun features(
        syncRequiredLevel: Int,
        vararg levels: CloudAPI.SubscriptionLevel,
    ): CloudAPI.UserFeatures = CloudAPI.UserFeatures().apply {
        this.syncRequiredLevel = syncRequiredLevel
        this.levels = levels.toMutableList()
    }

    private fun subscription(level: Int, url: String): CloudAPI.SubscriptionLevel =
        CloudAPI.SubscriptionLevel().apply {
            this.level = level
            manageUrl = url
        }
}
