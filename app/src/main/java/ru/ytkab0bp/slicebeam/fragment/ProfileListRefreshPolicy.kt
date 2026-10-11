package ru.ytkab0bp.slicebeam.fragment

/** Detects when a profile screen must rebind to shared configuration changes. */
internal object ProfileListRefreshPolicy {
    fun profileChanged(previousTitle: String?, activeTitle: String?): Boolean =
        activeTitle != null && activeTitle != previousTitle

    fun needsReload(
        renderedTitle: String?,
        renderedConfigUid: Int,
        activeTitle: String?,
        currentConfigUid: Int,
    ): Boolean =
        profileChanged(renderedTitle, activeTitle) || renderedConfigUid != currentConfigUid
}
