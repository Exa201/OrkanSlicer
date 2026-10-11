package ru.ytkab0bp.slicebeam.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.core.graphics.withSave
import androidx.core.view.isNotEmpty
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.MirrorView

abstract class UnfoldMenu {
    @JvmField
    protected var fragment: BedFragment? = null

    private var isVisible = false
    private var isDismissing = false
    private var spring: SpringAnimation? = null
    private var updateListener: DynamicAnimation.OnAnimationUpdateListener? = null
    private var containerLayout: FrameLayout? = null
    private var innerView: View? = null
    private var onDismiss: Runnable? = null
    private var dimmView: View? = null
    private var rootView: FrameLayout? = null
    private var progress = 0f
    private var portraitLayout = true
    private var fromTranslationX = 0f
    private var fromTranslationY = 0f
    private var toTranslationX = 0f
    private var toTranslationY = 0f

    open fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
        ((if (portrait) into.height else into.width) * 0.6f).toInt()

    @CallSuper
    protected open fun onCreate() = Unit

    @CallSuper
    protected open fun onDestroy() {
        onDismiss?.run()
    }

    open fun setOnDismiss(onDismiss: Runnable?) {
        this.onDismiss = onDismiss
    }

    protected abstract fun onCreateView(ctx: Context, portrait: Boolean): View

    open fun show(from: View, fragment: BedFragment) {
        show(from, fragment, fragment.overlayLayout ?: return)
    }

    open fun show(from: View, into: FrameLayout) {
        show(from, null, into)
    }

    private fun show(fromView: View, fragment: BedFragment?, into: FrameLayout) {
        if (isVisible) return
        if (fragment != null) this.fragment = fragment
        isVisible = true
        containerLayout = into

        // Avoid mirroring the overlay into itself when opened from a screen-sized view.
        var from = fromView
        if (isAncestorOf(from, into)) {
            var safeSource: View? = this.fragment?.glView
            if (safeSource == null || safeSource === into || isAncestorOf(safeSource, into)) {
                safeSource = if (into.isNotEmpty()) into.getChildAt(0) else null
            }
            if (safeSource != null) from = safeSource
        }

        val portrait = into.width < into.height
        portraitLayout = portrait
        val context = into.context
        val mirror = MirrorView(context).apply {
            setMirroredView(from)
            layoutParams = FrameLayout.LayoutParams(from.width, from.height)
        }

        val position = IntArray(2)
        from.getLocationInWindow(position)
        val intoPosition = IntArray(2)
        into.getLocationInWindow(intoPosition)
        intoPosition[0] += into.paddingLeft
        intoPosition[1] += into.paddingTop

        val side = getSideSize(into, portrait)
        fromTranslationX = (position[0] - intoPosition[0]).toFloat()
        fromTranslationY = (position[1] - intoPosition[1]).toFloat()
        toTranslationX = 0f
        toTranslationY = if (portrait) {
            (into.height - side - into.paddingTop - into.paddingBottom).toFloat()
        } else {
            0f
        }

        val root = object : FrameLayout(context) {
            private val path = Path()

            init {
                setWillNotDraw(false)
                isClickable = true
            }

            override fun draw(canvas: Canvas) {
                canvas.withSave {
                    path.rewind()
                    val radius = ViewUtils.dp(16) * (1f - progress)
                    path.addRoundRect(
                        0f,
                        0f,
                        ViewUtils.lerp(mirror.width.toFloat(), width.toFloat(), progress),
                        ViewUtils.lerp(mirror.height.toFloat(), height.toFloat(), progress),
                        radius,
                        radius,
                        Path.Direction.CW,
                    )
                    clipPath(path)
                    drawColor(ThemesRepo.getColor(android.R.attr.windowBackground))
                    super.draw(this)
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_UP) return performClick()
                return true
            }

            override fun performClick(): Boolean {
                super.performClick()
                dismiss()
                return true
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                if (isVisible) onCreate()
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                if (isVisible) onDestroy()
            }
        }
        rootView = root

        root.addView(mirror)
        val menuView = onCreateView(context, portrait)
        innerView = menuView
        root.addView(
            menuView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        menuView.alpha = 0f
        root.translationX = fromTranslationX
        root.translationY = fromTranslationY

        val dim = View(context).apply {
            setBackgroundColor(0x40000000)
            translationX = toTranslationX
            translationY = toTranslationY
            alpha = 0f
        }
        dimmView = dim
        val layoutParams = FrameLayout.LayoutParams(
            if (portrait) ViewGroup.LayoutParams.MATCH_PARENT else side,
            if (portrait) side else ViewGroup.LayoutParams.MATCH_PARENT,
        )
        into.addView(dim, layoutParams)
        into.addView(root, layoutParams)

        val listener = DynamicAnimation.OnAnimationUpdateListener { _, value, _ ->
            progress = value
            root.invalidate()
            dim.alpha = value
            root.translationX = ViewUtils.lerp(fromTranslationX, toTranslationX, value)
            root.translationY = ViewUtils.lerp(fromTranslationY, toTranslationY, value)

            val mirrorValue = minOf(0.75f, value) / 0.75f
            mirror.alpha = (1f - mirrorValue) * (mirror.getMirroredView()?.alpha ?: 1f)
            mirror.scaleX = 1f + mirrorValue
            mirror.scaleY = 1f + mirrorValue
            mirror.translationX = (root.width - mirror.width) / 2f * mirrorValue
            mirror.translationY = (root.height - mirror.height) / 2f * mirrorValue

            menuView.translationX = (mirror.width - menuView.width) * (1f - value)
            menuView.translationY = (mirror.height - menuView.height) * (1f - value)
            menuView.pivotX = menuView.width / 2f
            menuView.pivotY = menuView.height / 2f
            menuView.scaleX = 0.5f + value * 0.5f
            menuView.scaleY = 0.5f + value * 0.5f
            menuView.alpha = value

            val activeFragment = this.fragment
            if (activeFragment != null) {
                val glView = activeFragment.glView
                val snackbars = activeFragment.snackbarsLayout
                val inverseY = into.height - ViewUtils.dp(80 * 2) - toTranslationY
                if (!portraitLayout) {
                    val translationX = root.width - ViewUtils.dp(80 * 2)
                    glView?.translationX = translationX / 2f * value
                    val margins = snackbars?.layoutParams as? ViewGroup.MarginLayoutParams
                    if (margins != null) {
                        margins.leftMargin = (ViewUtils.dp(80 * 2) + translationX * value).toInt()
                        snackbars.requestLayout()
                    }
                    dim.translationX = root.translationX - ViewUtils.lerp(root.width - mirror.width.toFloat(), 0f, progress)
                } else {
                    glView?.translationY = -inverseY / 2f * value
                    snackbars?.translationY = -inverseY * value
                    dim.translationY = root.translationY
                }
                glView?.invalidate()
            }
        }
        updateListener = listener
        spring = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1f / 256f)
            .setSpring(
                SpringForce(1f)
                    .setStiffness(1000f)
                    .setDampingRatio(1f),
            )
            .addUpdateListener(listener)
            .addEndListener { _, _, _, _ -> onShown() }
        spring?.start()
    }

    private fun isAncestorOf(candidate: View, descendant: View): Boolean {
        var current: View? = descendant
        while (current != null) {
            if (current === candidate) return true
            current = current.parent as? View
        }
        return false
    }

    protected open fun onShown() = Unit

    private fun getSideSize(into: FrameLayout, portrait: Boolean): Int {
        val available = if (portrait) {
            into.height - into.paddingTop - into.paddingBottom
        } else {
            into.width - into.paddingLeft - into.paddingRight
        }
        return UnfoldMenuLayoutPolicy.constrainSide(getRequestedSize(into, portrait), available)
    }

    open fun relayout() {
        val into = containerLayout ?: return
        val root = rootView ?: return
        val dim = dimmView ?: return
        val listener = updateListener ?: return
        val portrait = into.width < into.height
        portraitLayout = portrait
        val side = getSideSize(into, portrait)
        toTranslationY = if (portrait) {
            (into.height - side - into.paddingTop - into.paddingBottom).toFloat()
        } else {
            0f
        }
        val params = FrameLayout.LayoutParams(
            if (portrait) ViewGroup.LayoutParams.MATCH_PARENT else side,
            if (portrait) side else ViewGroup.LayoutParams.MATCH_PARENT,
        )
        root.layoutParams = params
        dim.layoutParams = FrameLayout.LayoutParams(params.width, params.height)
        root.requestLayout()
        dim.requestLayout()
        listener.onAnimationUpdate(spring ?: return, progress, 0f)
    }

    open fun isAttached(): Boolean = rootView?.parent != null && !isDismissing

    open fun dismiss() = dismiss(false)

    open fun dismiss(alphaOnly: Boolean) {
        if (!isVisible) return
        isVisible = false
        isDismissing = true
        if (alphaOnly) {
            // The immediate fade skips the spring's return-to-zero frames. Restore the workspace
            // transforms before removing the overlay or the build plate remains translated off-screen.
            spring?.cancel()
            fragment?.restoreWorkspaceAfterMenuDismiss()
        }
        onDestroy()
        isDismissing = false

        val root = rootView ?: return
        val dim = dimmView ?: return
        val container = containerLayout ?: return
        if (alphaOnly) {
            val animation = ValueAnimator.ofFloat(0f, 1f).setDuration(150).apply {
                interpolator = ViewUtils.CUBIC_INTERPOLATOR
                addUpdateListener { update ->
                    val value = update.animatedValue as Float
                    root.alpha = 1f - value
                    dim.alpha = 1f - value
                    root.translationY = value * ViewUtils.dp(64)
                    dim.translationY = value * ViewUtils.dp(64)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(dim)
                        container.removeView(root)
                    }
                })
            }
            animation.start()
        } else {
            val activeSpring = spring ?: return
            activeSpring.spring.finalPosition = 0f
            activeSpring.addEndListener { _, _, _, _ ->
                fragment?.restoreWorkspaceAfterMenuDismiss()
                container.removeView(dim)
                container.removeView(root)
            }
            activeSpring.start()
        }
    }
}

/** Bounds full-screen and side-sheet menus to the drawable app window. */
internal object UnfoldMenuLayoutPolicy {
    fun constrainSide(requestedSize: Int, availableSize: Int): Int =
        requestedSize.coerceAtLeast(0).coerceAtMost(availableSize.coerceAtLeast(0))
}
