package ru.ytkab0bp.slicebeam.ui.workspace

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import ru.ytkab0bp.slicebeam.view.GLView

/** Bounds the existing SurfaceView to the shell's measured plate viewport without recreating it. */
internal class RendererAdapter(context: Context, renderer: GLView) : FrameLayout(context) {
    init {
        addView(
            renderer,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }
}
