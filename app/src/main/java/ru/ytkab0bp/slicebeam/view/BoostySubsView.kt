package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.text.TextPaint
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import androidx.core.math.MathUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

class BoostySubsView(context: Context) : View(context) {
    private val paint = TextPaint()
    private var entries: List<String> = emptyList()
    private val ellipsizedStrings = SparseArray<CharSequence>()
    private var index = 0
    private var progress = 0f
    private var lastUpdated = 0L
    private var firstHeight = 0
    private val bounds = Rect()

    init {
        paint.textSize = ViewUtils.dp(20).toFloat()
        paint.typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        updateColors()
    }

    fun setStrings(strings: List<String>) {
        entries = strings
        ellipsizedStrings.clear()
        index = 0
        progress = 0f
        if (entries.isNotEmpty()) {
            val first = entries[index]
            paint.getTextBounds(first, 0, first.length, bounds)
            firstHeight = bounds.height()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val elapsed = minOf(16L, System.currentTimeMillis() - lastUpdated)
        lastUpdated = System.currentTimeMillis()

        if (entries.isEmpty()) return

        val translateY = (ViewUtils.dp(24) + firstHeight) * progress
        val saveCount = canvas.save()
        try {
            canvas.translate(0f, -translateY)
            val halfHeight = height / 2f
            var y = 0
            var entryIndex = index
            while (y <= height + translateY) {
                val wrappedIndex = Math.floorMod(entryIndex, entries.size)
                val text = ellipsizedStrings[wrappedIndex] ?: TextUtils.ellipsize(
                    entries[wrappedIndex],
                    paint,
                    (width - paddingLeft - paddingRight).toFloat(),
                    TextUtils.TruncateAt.END,
                ).also { ellipsizedStrings.put(wrappedIndex, it) }

                val plainText = text.toString()
                paint.getTextBounds(plainText, 0, plainText.length, bounds)
                val highlight = (1f - kotlin.math.abs(
                    (y - translateY - firstHeight / 2f - halfHeight) / halfHeight,
                )).coerceIn(0f, 1f)
                paint.alpha = (0xFF * highlight).toInt()

                val x = (width - bounds.width()) / 2f
                canvas.drawText(text, 0, text.length, x, y.toFloat(), paint)

                y += bounds.height() + ViewUtils.dp(24)
                entryIndex++
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }

        progress += elapsed / 2000f
        if (progress > 1f) {
            progress -= 1f
            index = (index + 1) % entries.size

            val first = entries[index]
            paint.getTextBounds(first, 0, first.length, bounds)
            firstHeight = bounds.height()
        }
        invalidate()
    }

    fun updateColors() {
        paint.color = ThemesRepo.getColor(R.attr.textColorOnAccent)
        invalidate()
    }
}
