package ru.ytkab0bp.slicebeam.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextPaint
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.VibrationUtils
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import kotlin.math.abs

/** A stepped, animated ruler used to edit model position, rotation, and layer ranges. */
class PositionScrollView(context: Context) : View(context), IThemeView {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = ViewUtils.dp(2.5f).toFloat()
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = ViewUtils.dp(14).toFloat()
    }

    private var currentPosition = 0
    private var progress = 0f
    private var listener: Consumer<Int>? = null
    private var progressListener: Consumer<Int>? = null
    private val leftDrawable = GradientDrawable()
    private val rightDrawable = GradientDrawable()
    private var lastX = 0
    private val gestureScroller = Scroller(context)
    private var stepAnimation: SpringAnimation? = null
    private var wasProgress = 0f
    private var activeColor = android.R.attr.colorAccent
    private var inactiveColor = android.R.attr.textColorSecondary
    private var notClick = false
    private var min = Int.MIN_VALUE
    private var max = Int.MAX_VALUE
    private val synchronizedScrolls = mutableListOf<PositionScrollView>()
    private val syncDeltas = mutableMapOf<PositionScrollView, Int>()

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private var gotOffset = false

        override fun onDown(event: MotionEvent): Boolean {
            stepAnimation?.cancel()
            gotOffset = false
            if (!gestureScroller.isFinished) notClick = true
            gestureScroller.forceFinished(true)
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScroll(
            first: MotionEvent?,
            current: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            stepAnimation?.cancel()
            if (!gotOffset) {
                gotOffset = true
            } else {
                scrollDx(distanceX)
            }
            return true
        }

        override fun onFling(
            first: MotionEvent?,
            current: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            gestureScroller.fling(
                0,
                0,
                (-velocityX * 2f).toInt(),
                0,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                0,
                0,
            )
            lastX = 0
            ViewCompat.postInvalidateOnAnimation(this@PositionScrollView)
            return true
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean = !notClick && callOnClick()
    })

    init {
        onApplyTheme()
    }

    fun addSynced(scroll: PositionScrollView) {
        syncDeltas[scroll] = scroll.getCurrentPosition() - currentPosition
        synchronizedScrolls.add(scroll)
    }

    fun removeSynced(scroll: PositionScrollView) {
        syncDeltas.remove(scroll)
        synchronizedScrolls.remove(scroll)
    }

    fun updateSyncDeltas() {
        synchronizedScrolls.forEach { scroll ->
            syncDeltas[scroll] = scroll.getCurrentPosition() - currentPosition
        }
    }

    fun setActiveColor(activeColor: Int) {
        this.activeColor = activeColor
        invalidate()
    }

    fun setInactiveColor(inactiveColor: Int) {
        this.inactiveColor = inactiveColor
        invalidate()
    }

    fun setCurrentPosition(currentPosition: Int) {
        setCurrentPosition(currentPosition, false)
    }

    private fun onSetProgress(progress: Float) {
        this.progress = progress
        synchronizedScrolls.forEach { scroll ->
            val delta = syncDeltas.getValue(scroll)
            scroll.progress = if (currentPosition + delta <= scroll.min && delta < 0) 0f else progress
            scroll.invalidate()
        }
    }

    private fun onSetCurrentPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
        synchronizedScrolls.forEach { scroll ->
            val delta = syncDeltas.getValue(scroll)
            scroll.currentPosition = maxOf(currentPosition + delta, scroll.min)
            scroll.invalidate()
        }
    }

    fun setCurrentPosition(currentPosition: Int, notify: Boolean) {
        onSetCurrentPosition(currentPosition)
        onSetProgress(0f)
        if (notify) listener?.accept(currentPosition)
        invalidate()
    }

    fun stopScroll() {
        gestureScroller.forceFinished(true)
        computeScroll()
    }

    private fun scrollDx(dx: Float): Boolean {
        if ((currentPosition == min && dx < 0) || (currentPosition == max && dx > 0)) {
            invalidate()
            return false
        }

        val progressDelta = -dx / ViewUtils.dp(STEP_DP).toFloat()
        onSetProgress(progress + progressDelta)
        if ((progress > 0f && currentPosition == min) || (progress < 0f && currentPosition == max)) {
            onSetProgress(0f)
        }

        while (progress >= 1f) {
            onSetProgress(progress - 1f)
            if (currentPosition > min) {
                onSetCurrentPosition(currentPosition - STEP)
                notifyNewCurrent()
            }
        }
        while (progress <= -1f) {
            onSetProgress(progress + 1f)
            if (currentPosition < max) {
                onSetCurrentPosition(currentPosition + STEP)
                notifyNewCurrent()
            }
        }
        invalidate()
        return true
    }

    override fun computeScroll() {
        super.computeScroll()

        if (gestureScroller.computeScrollOffset()) {
            val dx = gestureScroller.currX - lastX
            lastX = gestureScroller.currX
            if (!scrollDx(dx.toFloat())) {
                gestureScroller.forceFinished(true)
                lastX = 0
                stepAnimation?.cancel()
                wasProgress = 0f
                stepAnimation = SpringAnimation(FloatValueHolder(progress))
                    .setMinimumVisibleChange(1 / 500f)
                    .setSpring(
                        SpringForce(0f)
                            .setStiffness(400f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY),
                    )
                    .setStartVelocity(
                        MathUtils.clamp(
                            -(gestureScroller.currVelocity / 1500f) / ViewUtils.dp(STEP_DP),
                            -6f,
                            6f,
                        ),
                    )
                    .addUpdateListener { _, value, _ ->
                        onSetProgress(value)
                        invalidate()
                    }
                    .addEndListener { _, _, _, _ -> finishStepIfNeeded() }
                    .addEndListener { _, _, _, _ ->
                        stepAnimation = null
                        listener?.accept(currentPosition)
                    }
                stepAnimation?.start()
            }
        } else if (lastX != 0) {
            onScrollFinished()
        }
    }

    private fun onScrollFinished() {
        lastX = 0
        notClick = false
        stepAnimation?.cancel()
        stepAnimation = SpringAnimation(FloatValueHolder(progress.also { wasProgress = it }))
            .setMinimumVisibleChange(1 / 500f)
            .setSpring(
                SpringForce(if (progress > 0.75f) 1f else if (progress < -0.75f) -1f else 0f)
                    .setStiffness(400f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY),
            )
            .addUpdateListener { _, value, _ ->
                onSetProgress(value)
                invalidate()
            }
            .addEndListener { _, _, _, _ -> finishStepIfNeeded() }
            .addEndListener { _, _, _, _ ->
                stepAnimation = null
                listener?.accept(currentPosition)
            }
        stepAnimation?.start()
    }

    private fun finishStepIfNeeded() {
        when {
            progress >= 1f -> {
                onSetProgress(progress - 1f)
                onSetCurrentPosition(currentPosition - STEP)
                notifyNewCurrent()
            }
            progress <= -1f -> {
                onSetProgress(progress + 1f)
                onSetCurrentPosition(currentPosition + STEP)
                notifyNewCurrent()
            }
        }
    }

    fun getCurrentPosition(): Int = currentPosition

    fun notifyNewCurrent() {
        progressListener?.accept(currentPosition)
        synchronizedScrolls.forEach { scroll ->
            scroll.progressListener?.accept(maxOf(currentPosition + syncDeltas.getValue(scroll), scroll.min))
        }
        vibrateHaptic()
        invalidate()
    }

    private fun vibrateHaptic() {
        if (
            Prefs.isVibrationEnabled() && VibrationUtils.hasAmplitudeControl() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        ) {
            performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }

    fun setProgressListener(progressListener: Consumer<Int>?) {
        this.progressListener = progressListener
    }

    fun setListener(listener: Consumer<Int>?) {
        this.listener = listener
    }

    fun setMinMax(min: Int, max: Int) {
        this.min = min
        this.max = max
        invalidate()
    }

    private fun resolveInactiveColor(): Int {
        val color = ThemesRepo.getColor(inactiveColor)
        return ColorUtils.setAlphaComponent(color, (Color.alpha(color) * 0.6f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val progressClamped = if (stepAnimation != null && wasProgress == 0f) 0f else MathUtils.clamp(progress, -1f, 1f)
        val transformProgress = abs(progressClamped)
        paint.color = ColorUtils.blendARGB(
            ThemesRepo.getColor(activeColor),
            resolveInactiveColor(),
            transformProgress,
        )

        val centerX = width / 2f + progress * ViewUtils.dp(STEP_DP)
        canvas.drawLine(
            centerX,
            ViewUtils.lerp(paint.strokeWidth, ViewUtils.dp(TOP_MARGIN_DP).toFloat(), transformProgress),
            centerX,
            height - ViewUtils.dp(BOTTOM_MARGIN_DP).toFloat(),
            paint,
        )
        drawTextIfNeeded(canvas, centerX, currentPosition, transformProgress)

        var x = centerX
        var position = currentPosition
        while (x > paddingLeft && position > min) {
            x -= ViewUtils.dp(STEP_DP)
            position -= STEP
            val stepProgress = if (position == currentPosition - STEP) maxOf(0f, progressClamped) else 0f
            val top = ViewUtils.lerp(ViewUtils.dp(TOP_MARGIN_DP).toFloat(), 0f, stepProgress)
            paint.color = ColorUtils.blendARGB(resolveInactiveColor(), ThemesRepo.getColor(activeColor), stepProgress)
            canvas.drawLine(x, top, x, height.toFloat() - ViewUtils.dp(BOTTOM_MARGIN_DP), paint)
            drawTextIfNeeded(canvas, x, position, 1f - stepProgress)
        }

        x = centerX
        position = currentPosition
        while (x < width - paddingRight && position < max) {
            x += ViewUtils.dp(STEP_DP)
            position += STEP
            val stepProgress = if (position == currentPosition + STEP) -minOf(0f, progressClamped) else 0f
            val top = ViewUtils.lerp(ViewUtils.dp(TOP_MARGIN_DP).toFloat(), 0f, stepProgress)
            paint.color = ColorUtils.blendARGB(resolveInactiveColor(), ThemesRepo.getColor(activeColor), stepProgress)
            canvas.drawLine(x, top, x, height.toFloat() - ViewUtils.dp(BOTTOM_MARGIN_DP), paint)
            drawTextIfNeeded(canvas, x, position, 1f - stepProgress)
        }

        paint.color = ThemesRepo.getColor(android.R.attr.windowBackground)
        canvas.drawRect(0f, 0f, paddingLeft.toFloat(), height.toFloat(), paint)
        canvas.drawRect((width - paddingRight).toFloat(), 0f, width.toFloat(), height.toFloat(), paint)
        leftDrawable.setBounds(paddingLeft, 0, ViewUtils.dp(GRADIENT_DP), height)
        leftDrawable.draw(canvas)
        rightDrawable.setBounds(width - ViewUtils.dp(GRADIENT_DP), 0, width - paddingRight, height)
        rightDrawable.draw(canvas)
    }

    private fun drawTextIfNeeded(canvas: Canvas, x: Float, position: Int, alpha: Float) {
        if (position % MARK_STEP == 0 || position == min) {
            val text = position.toString()
            val textWidth = textPaint.measureText(text)
            val previousAlpha = textPaint.alpha
            textPaint.alpha = (alpha * previousAlpha).toInt()
            canvas.save()
            val scale = 0.85f + alpha * 0.15f
            canvas.scale(scale, scale, x - textWidth / 2f, height.toFloat() - ViewUtils.dp(BOTTOM_MARGIN_DP))
            canvas.drawText(text, x - textWidth / 2f, height - ViewUtils.dp(BOTTOM_MARGIN_DP) / 2f, textPaint)
            canvas.restore()
            textPaint.alpha = previousAlpha
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)
        if (
            (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) &&
            gestureScroller.isFinished
        ) {
            onScrollFinished()
        }
        return handled
    }

    override fun onApplyTheme() {
        textPaint.color = resolveInactiveColor()
        leftDrawable.apply {
            setGradientType(GradientDrawable.LINEAR_GRADIENT)
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
            colors = intArrayOf(ThemesRepo.getColor(android.R.attr.windowBackground), Color.TRANSPARENT)
        }
        rightDrawable.apply {
            setGradientType(GradientDrawable.LINEAR_GRADIENT)
            orientation = GradientDrawable.Orientation.RIGHT_LEFT
            colors = intArrayOf(ThemesRepo.getColor(android.R.attr.windowBackground), Color.TRANSPARENT)
        }
    }

    companion object {
        private const val STEP = 1
        private const val MARK_STEP = 1
        private val TOP_MARGIN_DP = ViewUtils.dp(6)
        private val BOTTOM_MARGIN_DP = ViewUtils.dp(14)
        private val STEP_DP = ViewUtils.dp(10)
        private val GRADIENT_DP = ViewUtils.dp(32)
    }
}
