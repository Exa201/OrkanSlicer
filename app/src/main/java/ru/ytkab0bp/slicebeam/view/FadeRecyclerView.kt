package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.view.View
import androidx.core.math.MathUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

open class FadeRecyclerView(context: Context) : RecyclerView(context), IThemeView {
    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topProgress = 0f
    private var bottomProgress = 0f
    private var overlayAlpha = 1f
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private var bitmapMode = false
    private val linearLayoutManager = LinearLayoutManager(context)

    init {
        layoutManager = linearLayoutManager
        setWillNotDraw(false)
        addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateFadeState()
                }
            },
        )
        onApplyTheme()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateFadeState()
    }

    private fun updateFadeState() {
        var newTopProgress = 0f
        var newBottomProgress = 0f
        val itemCount = adapter?.itemCount ?: 0
        val childCount = linearLayoutManager.childCount

        if (itemCount > 0 && childCount > 0) {
            val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
            val lastPosition = linearLayoutManager.findLastVisibleItemPosition()
            if (firstPosition >= 0 && firstPosition < itemCount &&
                lastPosition >= firstPosition && lastPosition < itemCount
            ) {
                if (firstPosition > 0) {
                    newTopProgress = 1f
                } else {
                    linearLayoutManager.findViewByPosition(firstPosition)?.let { firstChild ->
                        val fadeDistance = minOf(firstChild.height, ViewUtils.dp(HEIGHT_DP) / 2)
                        if (fadeDistance > 0) {
                            newTopProgress = MathUtils.clamp(
                                -firstChild.top / fadeDistance.toFloat(),
                                0f,
                                1f,
                            )
                        }
                    }
                }

                if (lastPosition < itemCount - 1) {
                    newBottomProgress = 1f
                } else {
                    linearLayoutManager.findViewByPosition(lastPosition)?.let { lastChild ->
                        val fadeDistance = minOf(lastChild.height, ViewUtils.dp(HEIGHT_DP) / 2)
                        if (fadeDistance > 0) {
                            newBottomProgress = MathUtils.clamp(
                                (lastChild.bottom - height) / fadeDistance.toFloat(),
                                0f,
                                1f,
                            )
                        }
                    }
                }
            }
        }

        if (topProgress != newTopProgress || bottomProgress != newBottomProgress) {
            topProgress = newTopProgress
            bottomProgress = newBottomProgress
            invalidate()
        }
    }

    /** Bitmap compositing is expensive; use it only when transparency is required. */
    fun setBitmapMode() {
        bitmapMode = true
        topPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        bottomPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        invalidateShaders()
    }

    fun setOverlayAlpha(alpha: Float) {
        overlayAlpha = MathUtils.clamp(alpha, 0f, 1f)
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (bitmapMode && (width <= 0 || height <= 0)) {
            super.draw(canvas)
            return
        }

        if (bitmapMode && (bitmap == null || bitmap?.width != width || bitmap?.height != height)) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(requireNotNull(bitmap))
        }

        val contentCanvas = if (bitmapMode) requireNotNull(bitmapCanvas) else canvas
        if (bitmapMode) bitmap?.eraseColor(Color.TRANSPARENT)
        super.draw(contentCanvas)

        if (topProgress > 0f) {
            val saveCount = contentCanvas.save()
            try {
                if (bitmapMode) {
                    contentCanvas.translate(
                        0f,
                        -ViewUtils.dp(HEIGHT_DP) * (1f - topProgress * overlayAlpha),
                    )
                } else {
                    topPaint.alpha = (topProgress * overlayAlpha * 0xFF).toInt()
                }
                contentCanvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    ViewUtils.dp(HEIGHT_DP).toFloat(),
                    topPaint,
                )
            } finally {
                contentCanvas.restoreToCount(saveCount)
            }
        }

        if (bottomProgress > 0f) {
            val saveCount = contentCanvas.save()
            try {
                if (bitmapMode) {
                    contentCanvas.translate(
                        0f,
                        ViewUtils.dp(HEIGHT_DP) * (1f - bottomProgress * overlayAlpha),
                    )
                } else {
                    bottomPaint.alpha = (bottomProgress * overlayAlpha * 0xFF).toInt()
                }
                contentCanvas.drawRect(
                    0f,
                    (height - ViewUtils.dp(HEIGHT_DP)).toFloat(),
                    width.toFloat(),
                    height.toFloat(),
                    bottomPaint,
                )
            } finally {
                contentCanvas.restoreToCount(saveCount)
            }
        }

        if (bitmapMode) {
            canvas.drawBitmap(requireNotNull(bitmap), 0f, 0f, null)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bitmap?.recycle()
        bitmap = null
        bitmapCanvas = null
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        invalidateShaders()
    }

    private fun invalidateShaders() {
        if (width == 0 || height == 0) return

        val color = if (bitmapMode) Color.BLACK else ThemesRepo.getColor(android.R.attr.windowBackground)
        topPaint.shader = LinearGradient(
            width / 2f,
            0f,
            width / 2f,
            ViewUtils.dp(HEIGHT_DP).toFloat(),
            if (bitmapMode) 0 else color,
            if (bitmapMode) color else 0,
            Shader.TileMode.CLAMP,
        )
        bottomPaint.shader = LinearGradient(
            width / 2f,
            (height - ViewUtils.dp(HEIGHT_DP)).toFloat(),
            width / 2f,
            height.toFloat(),
            if (bitmapMode) color else 0,
            if (bitmapMode) 0 else color,
            Shader.TileMode.CLAMP,
        )
        invalidate()
    }

    override fun onApplyTheme() {
        invalidateShaders()
    }

    private companion object {
        const val HEIGHT_DP = 32
    }
}
