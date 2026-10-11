package ru.ytkab0bp.slicebeam.ui

import android.content.Context
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.ytkab0bp.slicebeam.theme.IThemeView

/** Compose root that participates directly in the app's explicit light/dark theme invalidation. */
open class ThemeAwareComposeView(context: Context) : AbstractComposeView(context), IThemeView {
    var themeRevision by mutableIntStateOf(0)
        private set

    private var contentState: (@Composable () -> Unit)? by mutableStateOf(null)

    fun setContent(content: @Composable () -> Unit) {
        contentState = content
    }

    @Composable
    override fun Content() {
        val revision = themeRevision
        if (revision >= 0) contentState?.invoke()
    }

    override fun onApplyTheme() {
        themeRevision++
    }
}
