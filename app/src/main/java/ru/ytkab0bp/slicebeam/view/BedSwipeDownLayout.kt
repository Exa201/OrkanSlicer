package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Workspace gesture surface that reveals or hides the top tool area with a vertical swipe. */
class BedSwipeDownLayout(context: Context) : FrameLayout(context), IThemeView {
    fun interface OnTopVisibilityChangedListener {
        fun onTopVisibilityChanged(visible: Boolean)
    }

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ViewUtils.dp(3).toFloat()
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(ViewUtils.dp(6).toFloat())
    }
    private val dimmPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var openEnabled = true
    private var topVisible = false
    private var onTopVisibilityChangedListener: OnTopVisibilityChangedListener? = null
    private var processingSwipe = false
    private var springAnimation: SpringAnimation? = null
    private var startProgress = 0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean {
            if (springAnimation != null) return false
            processingSwipe = true
            startProgress = progress
            return true
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            if (springAnimation != null) return false
            animateTo(1f)
            return true
        }

        override fun onScroll(
            first: MotionEvent?,
            current: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            if (springAnimation != null) return false
            if (processingSwipe) {
                progress = MathUtils.clamp(
                    startProgress + (current.y - first!!.y) / height,
                    0f,
                    1f,
                )
                setTopVisible(progress >= 0.5f)
                invalidateTranslation()
                return true
            }
            return false
        }

        override fun onFling(
            first: MotionEvent?,
            current: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            if (springAnimation != null) return false
            if (velocityY >= 1500) {
                animateTo(1f)
                return true
            }
            return false
        }
    })

    init {
        setWillNotDraw(false)
        onApplyTheme()
    }

    fun onBackPressed(): Boolean {
        if (springAnimation != null) return true
        if (progress == 1f) {
            animateTo(0f)
            return true
        }
        return false
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (openEnabled) {
            if (processingSwipe) {
                val handled = gestureDetector.onTouchEvent(event)
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    processingSwipe = false
                    if (springAnimation == null && progress != 0f && progress != 1f) {
                        animateTo(if (progress > 0.5f) 1f else 0f)
                    }
                }
                return handled
            }

            if (progress != 1f && springAnimation == null) {
                if (event.y < ViewUtils.dp(TOP_MARGIN_DP) + height * progress) {
                    return gestureDetector.onTouchEvent(event)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val index = indexOfChild(child)
        canvas.save()
        val translationY = -height * (1f - progress)
        if (openEnabled && index != 0) {
            dimmPaint.color = ThemesRepo.getColor(android.R.attr.windowBackground)
            canvas.drawRect(
                0f,
                child.top + translationY,
                child.width.toFloat(),
                child.top + translationY + child.height,
                dimmPaint,
            )
            canvas.clipRect(
                0f,
                child.top + translationY,
                child.width.toFloat(),
                child.top + translationY + child.height,
            )
        }

        val drawn = super.drawChild(canvas, child, drawingTime)
        if (openEnabled && index == 0) {
            dimmPaint.color = Color.argb((0x66 * progress).toInt(), 0, 0, 0)
            canvas.drawPaint(dimmPaint)
        }
        canvas.restore()
        return drawn
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (openEnabled) {
            path.rewind()
            val centerX = width * 0.5f
            val centerY = ViewUtils.dp(TOP_MARGIN_DP) * 0.5f + height * progress
            val angle = Math.toRadians(25.0).toFloat() * (1f - min(progress, 0.5f) / 0.5f)
            val length = ViewUtils.dp(ARROW_LENGTH_DP)
            path.moveTo(centerX, centerY)
            path.lineTo(centerX - cos(angle) * length, centerY - sin(angle) * length)
            path.moveTo(centerX, centerY)
            path.lineTo(centerX + cos(angle) * length, centerY - sin(angle) * length)
            canvas.drawPath(path, paint)
        }
    }

    private fun animateTo(target: Float) {
        setTopVisible(target >= 0.5f)
        springAnimation = SpringAnimation(FloatValueHolder(progress))
            .setMinimumVisibleChange(1 / 500f)
            .setSpring(
                SpringForce(target)
                    .setStiffness(600f)
                    .setDampingRatio(1f),
            )
            .addUpdateListener { _, value, _ ->
                progress = value
                invalidateTranslation()
            }
            .addEndListener { _, _, _, _ ->
                springAnimation = null
                processingSwipe = false
            }
        springAnimation?.start()
    }

    private fun invalidateTranslation() {
        if (childCount > 0) {
            getChildAt(0).translationY = height * progress * 0.25f
            for (index in 1 until childCount) {
                val child = getChildAt(index)
                child.translationY = -height * (1f - progress) * 0.75f
                child.alpha = progress
            }
        }
        invalidate()
    }

    fun setEnableTop(enable: Boolean) {
        if (enable == openEnabled) return
        openEnabled = enable
        if (!enable) {
            springAnimation?.cancel()
            progress = 0f
            setTopVisible(false)
            invalidateTranslation()
        }
        requestLayout()
    }

    fun isTopVisible(): Boolean = topVisible

    fun setOnTopVisibilityChangedListener(listener: OnTopVisibilityChangedListener?) {
        onTopVisibilityChangedListener = listener
    }

    private fun setTopVisible(visible: Boolean) {
        if (topVisible == visible) return
        topVisible = visible
        onTopVisibilityChangedListener?.onTopVisibilityChanged(visible)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )

        if (childCount > 0) {
            getChildAt(0).measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                    height - ViewUtils.dp(TOP_MARGIN_DP) * if (openEnabled) 1 else 0,
                    View.MeasureSpec.EXACTLY,
                ),
            )
            for (index in 1 until childCount) {
                getChildAt(index).measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                )
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (openEnabled && childCount > 0) {
            val first = getChildAt(0)
            val margin = ViewUtils.dp(TOP_MARGIN_DP)
            first.layout(0, margin, first.measuredWidth, margin + first.measuredHeight)
            for (index in 1 until childCount) {
                val child = getChildAt(index)
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
        invalidateTranslation()
    }

    override fun onApplyTheme() {
        paint.color = ColorUtils.setAlphaComponent(
            ThemesRepo.getColor(android.R.attr.colorControlHighlight),
            0x44,
        )
    }

    companion object {
        private const val TOP_MARGIN_DP = 28
        private const val ARROW_LENGTH_DP = 11
    }
}
