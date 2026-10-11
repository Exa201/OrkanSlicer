package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

class DividerView : View, IThemeView {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var colorRes: Int

    constructor(context: Context) : this(context, R.attr.dividerColor)

    constructor(context: Context, colorRes: Int) : super(context) {
        this.colorRes = colorRes
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        onApplyTheme()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPaint(paint)
    }

    override fun onApplyTheme() {
        paint.color = ThemesRepo.getColor(colorRes)
    }
}
