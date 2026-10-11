package ru.ytkab0bp.slicebeam.recycler

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem

class SpaceItem(@JvmField val x: Int, @JvmField val y: Int) : ProfileSettingsComposeItem {
    @Composable
    override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
        val density = LocalDensity.current
        Spacer(
            modifier = Modifier
                .widthIn(min = with(density) { x.toDp() })
                .height(with(density) { y.toDp() }),
        )
    }
}
