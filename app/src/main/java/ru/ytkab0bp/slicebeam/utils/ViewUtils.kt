package ru.ytkab0bp.slicebeam.utils

import android.content.res.ColorStateList
import android.animation.TimeInterpolator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.animation.PathInterpolator
import ru.ytkab0bp.slicebeam.SliceBeam

/** Shared Android view helpers used by Java and Kotlin screens. */
class ViewUtils {
    companion object {
        @JvmField
        val CUBIC_INTERPOLATOR: TimeInterpolator = PathInterpolator(0.25f, 0f, 0.25f, 1f)

        @JvmField
        val ROBOTO_MEDIUM: String = "roboto_medium"

        private val uiHandler = Handler(Looper.getMainLooper())
        private val typefaceCache = HashMap<String, Typeface>()

        @JvmStatic
        fun getUiHandler(): Handler = uiHandler

        @JvmStatic
        fun postOnMainThread(runnable: Runnable) {
            uiHandler.post(runnable)
        }

        @JvmStatic
        fun postOnMainThread(runnable: Runnable, delay: Long) {
            uiHandler.postDelayed(runnable, delay)
        }

        @JvmStatic
        fun removeCallbacks(runnable: Runnable) {
            uiHandler.removeCallbacks(runnable)
        }

        @JvmStatic
        fun getTypeface(key: String): Typeface = typefaceCache.getOrPut(key) {
            Typeface.createFromAsset(SliceBeam.requireInstance().assets, "font/$key.ttf")
        }

        @JvmStatic
        fun lerp(a: Float, b: Float, progress: Float): Float = a + (b - a) * progress

        @JvmStatic
        fun lerpd(a: Double, b: Double, c: Double, progress: Float): Double = lerpd(
            lerpd(a, b, minOf(progress, 0.5f) / 0.5f),
            c,
            (maxOf(progress, 0.5f) - 0.5f) / 0.5f,
        )

        @JvmStatic
        fun lerpd(a: Double, b: Double, progress: Float): Double = a + (b - a) * progress

        @JvmStatic
        fun createRipple(color: Int, radiusDp: Float): RippleDrawable = createRipple(color, 0, radiusDp)

        @JvmStatic
        fun createRipple(color: Int, fillColor: Int, radiusDp: Float): RippleDrawable {
            if (radiusDp == -1f) return RippleDrawable(ColorStateList.valueOf(color), null, null)

            val radius = dp(radiusDp).toFloat()
            val mask = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = radius
            }
            val content = if (fillColor != 0) {
                GradientDrawable().apply {
                    setColor(fillColor)
                    cornerRadius = radius
                }
            } else {
                null
            }
            return RippleDrawable(ColorStateList.valueOf(color), content, mask)
        }

        @JvmStatic
        fun dp(dp: Float): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            SliceBeam.requireInstance().resources.displayMetrics,
        ).toInt()

        /** Keeps the Java overload available to Kotlin and Java call sites using integer literals. */
        @JvmStatic
        fun dp(dp: Int): Int = dp(dp.toFloat())
    }
}
