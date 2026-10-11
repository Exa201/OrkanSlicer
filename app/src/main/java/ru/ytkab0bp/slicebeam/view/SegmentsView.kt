package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

open class SegmentsView(context: Context) : View(context) {
    private val path = Path()
    private val paint = Paint()
    private var showProgress = 0f
    private var isVisible = false
    private var currentValues: FloatArray? = null
    private var currentChangeAnimation: SpringAnimation? = null

    fun startAnimation() {
        isVisible = true
        SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1f / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(1f))
            .addUpdateListener { _, value, _ ->
                showProgress = value
                invalidate()
            }
            .start()
    }

    fun setNotVisible() {
        isVisible = false
    }

    fun setValues(values: FloatArray) {
        val previousValues = currentValues
        if (!isVisible || previousValues == null || previousValues.size != values.size) {
            currentValues = values
            invalidate()
            return
        }

        val fromValues = previousValues.copyOf()
        currentChangeAnimation = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1f / 256f)
            .setSpring(SpringForce(1f).setStiffness(1000f).setDampingRatio(1f))
            .addUpdateListener { animation, value, _ ->
                val animatedValues = currentValues ?: return@addUpdateListener
                for (index in animatedValues.indices) {
                    animatedValues[index] = ViewUtils.lerp(fromValues[index], values[index], value)
                }
                invalidate()
            }
            .addEndListener { animation, _, _, _ ->
                if (animation == currentChangeAnimation) {
                    currentChangeAnimation = null
                }
            }
        currentChangeAnimation?.start()
    }

    protected open fun onGetColor(index: Int): Int = mapColor(index)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val saveCount = canvas.save()
        try {
            path.rewind()
            val left = 0f
            val right = ViewUtils.lerp(0f, width.toFloat(), showProgress)
            path.addRoundRect(
                left,
                0f,
                right,
                height.toFloat(),
                ViewUtils.dp(12).toFloat(),
                ViewUtils.dp(12).toFloat(),
                Path.Direction.CW,
            )
            canvas.clipPath(path)

            val values = currentValues ?: return
            val segmentWidth = right - left
            for (index in 1 until values.size) {
                val previous = values[index - 1]
                val next = values[index]
                paint.color = onGetColor(index - 1)
                canvas.drawRect(
                    left + previous * segmentWidth,
                    0f,
                    left + next * segmentWidth,
                    height.toFloat(),
                    paint,
                )
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    companion object {
        @JvmStatic
        fun mapColor(@GCodeViewer.ExtrusionRole index: Int): Int = when (index) {
            GCodeViewer.EXTRUSION_ROLE_PERIMETER -> ThemesRepo.getColor(R.attr.gcodeViewerPerimeter)
            GCodeViewer.EXTRUSION_ROLE_EXTERNAL_PERIMETER -> ThemesRepo.getColor(R.attr.gcodeViewerExternalPerimeter)
            GCodeViewer.EXTRUSION_ROLE_OVERHANG_PERIMETER -> ThemesRepo.getColor(R.attr.gcodeViewerOverhangPerimeter)
            GCodeViewer.EXTRUSION_ROLE_INTERNAL_INFILL -> ThemesRepo.getColor(R.attr.gcodeViewerInternalInfill)
            GCodeViewer.EXTRUSION_ROLE_SOLID_INFILL -> ThemesRepo.getColor(R.attr.gcodeViewerSolidInfill)
            GCodeViewer.EXTRUSION_ROLE_TOP_SOLID_INFILL -> ThemesRepo.getColor(R.attr.gcodeViewerTopSolidInfill)
            GCodeViewer.EXTRUSION_ROLE_IRONING -> ThemesRepo.getColor(R.attr.gcodeViewerIroning)
            GCodeViewer.EXTRUSION_ROLE_BRIDGE_INFILL -> ThemesRepo.getColor(R.attr.gcodeViewerBridgeInfill)
            GCodeViewer.EXTRUSION_ROLE_GAP_FILL -> ThemesRepo.getColor(R.attr.gcodeViewerGapFill)
            GCodeViewer.EXTRUSION_ROLE_SKIRT -> ThemesRepo.getColor(R.attr.gcodeViewerSkirt)
            GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL -> ThemesRepo.getColor(R.attr.gcodeViewerSupportMaterial)
            GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL_INTERFACE -> ThemesRepo.getColor(R.attr.gcodeViewerSupportMaterialInterface)
            GCodeViewer.EXTRUSION_ROLE_WIPE_TOWER -> ThemesRepo.getColor(R.attr.gcodeViewerWipeTower)
            GCodeViewer.EXTRUSION_ROLE_CUSTOM -> ThemesRepo.getColor(R.attr.gcodeViewerCustom)
            else -> ThemesRepo.getColor(R.attr.gcodeViewerNone)
        }
    }
}
