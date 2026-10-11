package ru.ytkab0bp.slicebeam.fragment

import androidx.compose.runtime.Composable

/** A single profile row rendered in the profile screen's Compose composition. */
interface ProfileSettingsComposeItem {
    @Composable
    fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit)
}
