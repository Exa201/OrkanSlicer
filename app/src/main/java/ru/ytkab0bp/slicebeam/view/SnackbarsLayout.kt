package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.components.SnackbarCard
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Host for timed, tagged, actionable, and progress snackbars. */
class SnackbarsLayout(context: Context) : FrameLayout(context) {
    fun show(snackbar: Snackbar) {
        snackbar.tag?.let(::dismiss)

        val view = SnackbarView(context).bind(snackbar)
        addView(view)
        applyTransforms()
        SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1 / 500f)
            .setSpring(
                SpringForce(1f)
                    .setStiffness(1000f)
                    .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY),
            )
            .addUpdateListener { _, value, _ ->
                view.progress = value
                applyTransforms()
            }
            .start()

        if (snackbar.lifetime > 0) {
            ViewUtils.postOnMainThread({ view.dismiss() }, snackbar.lifetime.toLong())
        }
    }

    fun dismiss(tag: String?) {
        for (index in 0 until childCount) {
            val snackbar = getChildAt(index) as SnackbarView
            if (snackbar.modelTag() == tag) snackbar.dismiss()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTransforms()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        applyTransforms()
    }

    private fun applyTransforms() {
        var y = height.toFloat() - ViewUtils.dp(8)
        for (index in childCount - 1 downTo 0) {
            val snackbar = getChildAt(index) as SnackbarView
            if (snackbar.tag == null) snackbar.alpha = snackbar.progress
            val yOffset = snackbar.alpha * snackbar.progress * (snackbar.height + ViewUtils.dp(8))
            y -= yOffset
            snackbar.translationY = y
        }
    }

    /** Keeps host layout/stack animations native while the complete card is composed. */
    private inner class SnackbarView(context: Context) : FrameLayout(context) {
        private val composeView = ThemeAwareComposeView(context)
        private var snackbarState by mutableStateOf<Snackbar?>(null)
        private lateinit var snackbar: Snackbar
        var progress = 0f

        init {
            elevation = ViewUtils.dp(4).toFloat()
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, width, height, ViewUtils.dp(16).toFloat())
                }
            }
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                val margin = ViewUtils.dp(MARGIN_DP)
                leftMargin = margin
                topMargin = margin
                rightMargin = margin
                bottomMargin = margin
            }
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            composeView.setContent {
                val revision = composeView.themeRevision
                snackbarState?.let { current ->
                    BeamComposeTheme(themeRevision = revision) {
                        SnackbarCard(
                            type = current.type,
                            title = current.title,
                            buttonTitle = current.buttonTitle,
                            onButtonClick = { current.buttonClick?.onClick(this@SnackbarView) },
                            onHorizontalDrag = ::dragBy,
                            onDragEnd = { velocityX -> finishDrag(current, velocityX) },
                            onDragCancel = { finishDrag(current, 0f) },
                        )
                    }
                }
            }
        }

        fun bind(snackbar: Snackbar): SnackbarView {
            this.snackbar = snackbar
            snackbarState = snackbar
            return this
        }

        private fun dragBy(distanceX: Float) {
            parent?.requestDisallowInterceptTouchEvent(true)
            translationX += distanceX
        }

        private fun finishDrag(snackbar: Snackbar, velocityX: Float) {
            val release = SnackbarSwipePolicy.release(
                lifetimeMs = snackbar.lifetime,
                velocityX = velocityX,
                viewWidth = width,
                marginPx = ViewUtils.dp(MARGIN_DP),
            )
            animateTo(release.targetX, remove = release.dismiss)
        }

        fun dismiss() {
            setTag(1)
            SpringAnimation(FloatValueHolder(0f))
                .setMinimumVisibleChange(1 / 500f)
                .setSpring(
                    SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY),
                )
                .addUpdateListener { _, value, _ ->
                    alpha = 1f - value
                    applyTransforms()
                }
                .addEndListener { _, _, _, _ ->
                    (parent as? ViewGroup)?.removeView(this)
                }
                .start()
        }

        private fun animateTo(x: Float, remove: Boolean) {
            if (remove) setTag(1)
            val start = translationX
            SpringAnimation(FloatValueHolder(0f))
                .setMinimumVisibleChange(1 / 500f)
                .setSpring(
                    SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY),
                )
                .addUpdateListener { _, value, _ ->
                    translationX = ViewUtils.lerp(start, x, value)
                    if (remove) {
                        progress = 1f - value
                        applyTransforms()
                    }
                }
                .addEndListener { _, _, _, _ ->
                    if (remove) (parent as? ViewGroup)?.removeView(this@SnackbarView)
                }
                .start()
        }

        fun modelTag(): String? = snackbar.tag
    }

    class Snackbar(type: Type, title: CharSequence) {
        @JvmField var title: CharSequence = title
        @JvmField var type: Type = type
        @JvmField var lifetime = if (type == Type.WARNING || type == Type.ERROR) 5000 else 2500
        @JvmField var tag: String? = null
        @JvmField var buttonTitle: CharSequence? = null
        @JvmField var buttonClick: View.OnClickListener? = null

        fun tag(tag: String): Snackbar {
            lifetime = 0
            this.tag = tag
            return this
        }
    }

    enum class Type {
        DONE,
        WARNING,
        INFO,
        ERROR,
        LOADING,
    }

    private companion object {
        const val MARGIN_DP = 8
    }
}
