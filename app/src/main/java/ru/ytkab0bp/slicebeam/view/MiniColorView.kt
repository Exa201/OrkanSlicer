package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils

class MiniColorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selectionFraction = 0f

    init {
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = ViewUtils.dp(2f).toFloat()

        outlinePaintSecondary.style = Paint.Style.STROKE
        outlinePaintSecondary.strokeWidth = ViewUtils.dp(2f).toFloat()
        outlinePaintSecondary.color = ThemesRepo.getColor(R.attr.dividerColor)
    }

    fun setSelectionProgress(selectionProgress: Float) {
        this.selectionFraction = selectionProgress
        invalidate()
    }

    fun setColor(color: Int) {
        paint.color = color
        outlinePaint.color = color
        outlinePaintSecondary.color =
            if (ColorUtils.calculateLuminance(color) > 0.9) ThemesRepo.getColor(R.attr.dividerColor)
            else Color.TRANSPARENT
        selectionFraction = if (Prefs.getAccentColor() == color) 1f else 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height)
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            size / 2f - outlinePaintSecondary.strokeWidth / 2f,
            outlinePaintSecondary,
        )
        val strokeWidth = (outlinePaint.strokeWidth + outlinePaintSecondary.strokeWidth) / 2f
        canvas.drawCircle(width / 2f, height / 2f, size / 2f - strokeWidth, outlinePaint)
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            size / 2f - ViewUtils.lerp(0f, strokeWidth * 3f, selectionFraction),
            paint,
        )
    }
}
