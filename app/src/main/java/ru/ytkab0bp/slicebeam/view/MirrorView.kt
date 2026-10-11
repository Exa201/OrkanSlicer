package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.view.View

/** Draws a source view into the menu's snapshot area. */
class MirrorView(context: Context) : View(context) {
    private var mirrored: View? = null

    fun setMirroredView(mirrored: View?) {
        this.mirrored = mirrored
        invalidate()
    }

    fun getMirroredView(): View? = mirrored

    override fun onDraw(canvas: Canvas) {
        mirrored?.draw(canvas)
    }
}
