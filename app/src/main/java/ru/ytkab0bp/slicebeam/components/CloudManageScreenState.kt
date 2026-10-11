package ru.ytkab0bp.slicebeam.components

import ru.ytkab0bp.slicebeam.cloud.CloudAPI

/** Immutable snapshot for the account-management sheet. */
data class CloudManageScreenState(
    val accountName: String?,
    val isLoading: Boolean,
    val hasSubscription: Boolean,
    val subscriptionUrl: String?,
    val canSyncProfiles: Boolean,
    val syncEnabled: Boolean,
) {
    companion object {
        @JvmStatic
        fun resolve(
            userInfo: CloudAPI.UserInfo?,
            features: CloudAPI.UserFeatures?,
            syncEnabled: Boolean,
        ): CloudManageScreenState {
            if (userInfo == null || features == null) {
                return CloudManageScreenState(
                    accountName = userInfo?.displayName?.takeIf(String::isNotBlank),
                    isLoading = true,
                    hasSubscription = false,
                    subscriptionUrl = null,
                    canSyncProfiles = false,
                    syncEnabled = syncEnabled,
                )
            }
            val currentLevel = userInfo.currentLevel
            val activeLevel = features.levels
                .asSequence()
                .filter { it.level != -1 && it.level <= currentLevel }
                .maxByOrNull { it.level }
            return CloudManageScreenState(
                accountName = userInfo.displayName?.takeIf(String::isNotBlank),
                isLoading = false,
                hasSubscription = activeLevel != null,
                subscriptionUrl = activeLevel?.manageUrl?.takeIf(String::isNotBlank),
                canSyncProfiles = activeLevel != null && currentLevel >= features.syncRequiredLevel,
                syncEnabled = syncEnabled,
            )
        }
    }
}
