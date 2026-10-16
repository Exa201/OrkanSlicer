package ru.ytkab0bp.slicebeam

import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.view.GLView
import ru.ytkab0bp.slicebeam.view.PaintModeView

/** Guards the touch brush size control that must be visible during color painting. */
@RunWith(AndroidJUnit4::class)
class PaintModeViewDeviceTest {
    @Test
    fun colorPaintingExposesAccessibleBrushRadiusControl() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        lateinit var paintView: PaintModeView
        lateinit var radiusControl: SeekBar

        instrumentation.runOnMainSync {
            val glView = GLView(context)
            paintView = PaintModeView(context, glView, Runnable {}, GLRenderer.PAINT_MODE_COLOR)
            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels
            paintView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST),
            )
            paintView.layout(0, 0, paintView.measuredWidth, paintView.measuredHeight)
            val initialLabel = context.getString(R.string.PaintBrushRadius, 4f)
            radiusControl = findViews(paintView)
                .filterIsInstance<SeekBar>()
                .single { it.contentDescription == initialLabel }
            radiusControl.progress = 8
        }

        val updatedLabel = context.getString(R.string.PaintBrushRadius, 8f)
        assertEquals(View.VISIBLE, radiusControl.visibility)
        assertTrue(
            "brush radius slider should have a usable 48 dp touch height",
            radiusControl.height / context.resources.displayMetrics.density >= 48f,
        )
        assertEquals(updatedLabel, radiusControl.contentDescription)
    }

    private fun findViews(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) addAll(findViews(root.getChildAt(index)))
        }
    }
}
