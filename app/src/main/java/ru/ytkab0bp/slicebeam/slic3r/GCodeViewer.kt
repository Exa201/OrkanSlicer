package ru.ytkab0bp.slicebeam.slic3r

import android.graphics.Color
import androidx.annotation.IntDef
import androidx.core.util.Pair
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.DebugUtils

open class GCodeViewer {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [
            EXTRUSION_ROLE_NONE,
            EXTRUSION_ROLE_PERIMETER,
            EXTRUSION_ROLE_EXTERNAL_PERIMETER,
            EXTRUSION_ROLE_OVERHANG_PERIMETER,
            EXTRUSION_ROLE_INTERNAL_INFILL,
            EXTRUSION_ROLE_SOLID_INFILL,
            EXTRUSION_ROLE_TOP_SOLID_INFILL,
            EXTRUSION_ROLE_IRONING,
            EXTRUSION_ROLE_BRIDGE_INFILL,
            EXTRUSION_ROLE_GAP_FILL,
            EXTRUSION_ROLE_SKIRT,
            EXTRUSION_ROLE_SUPPORT_MATERIAL,
            EXTRUSION_ROLE_SUPPORT_MATERIAL_INTERFACE,
            EXTRUSION_ROLE_WIPE_TOWER,
            EXTRUSION_ROLE_CUSTOM,
        ],
    )
    annotation class ExtrusionRole

    private val viewMatrixBuffer = object : ThreadLocal<FloatArray>() {
        override fun initialValue() = FloatArray(16)
    }
    private val projectionMatrixBuffer = object : ThreadLocal<FloatArray>() {
        override fun initialValue() = FloatArray(16)
    }

    private var pointer: Long = Native.vgcode_create()

    fun isInitialized(): Boolean = Native.vgcode_is_initialized(pointer)

    fun initGL() = Native.vgcode_init(pointer)

    fun load(result: GCodeProcessorResult) = Native.vgcode_load(pointer, result.pointer)

    fun setLayersViewRange(min: Long, max: Long) = Native.vgcode_set_layers_view_range(pointer, min, max)

    fun setInfillVisibilityDepth(depth: Int) = Native.vgcode_set_infill_visibility_depth(pointer, depth)

    fun isOptionVisible(optionType: Int): Boolean = Native.vgcode_is_option_visible(pointer, optionType)

    fun toggleOptionVisibility(optionType: Int) = Native.vgcode_toggle_option_visibility(pointer, optionType)

    var viewType: Int
        get() = Native.vgcode_get_view_type(pointer)
        set(value) = Native.vgcode_set_view_type(pointer, value)

    /** Per-filament colors used by the Tool (filament) view; each int is 0xRRGGBB. */
    fun setToolColors(colors: IntArray) = Native.vgcode_set_tool_colors(pointer, colors)

    fun setFastMode(fastMode: Boolean) = Native.vgcode_set_fast_mode(pointer, fastMode)

    /** Call on the GL thread, together with layer-range changes. */
    fun applyPerformanceMode(enabled: Boolean) {
        setFastMode(enabled)
        setInfillVisibilityDepth(if (enabled) 5 else -1)
    }

    fun setSelectedObjectId(id: Int) = Native.vgcode_set_selected_object_id(pointer, id)

    val layersViewRange: Pair<Long, Long>
        get() {
            val data = Native.vgcode_get_layers_view_range(pointer)
            return Pair(data[0], if (data[1] < 0) getLayersCount() else data[1])
        }

    fun getLayersCount(): Long = Native.vgcode_get_layers_count(pointer)

    fun getEstimatedTime(): Float = Native.vgcode_get_estimated_time(pointer)

    fun getEstimatedTime(@ExtrusionRole extrusionRole: Int): Float =
        Native.vgcode_get_estimated_time_role(pointer, extrusionRole)

    fun isExtrusionRoleVisible(@ExtrusionRole extrusionRole: Int): Boolean =
        Native.vgcode_is_extrusion_role_visible(pointer, extrusionRole)

    fun toggleExtrusionRoleVisible(@ExtrusionRole extrusionRole: Int) =
        Native.vgcode_toggle_extrusion_role_visibility(pointer, extrusionRole)

    fun render(viewMatrix: DoubleArray, projectionMatrix: DoubleArray) {
        DebugUtils.assertTrue(viewMatrix.size == 16)
        DebugUtils.assertTrue(projectionMatrix.size == 16)

        val viewFloats = viewMatrixBuffer.get()
        for (index in viewMatrix.indices) viewFloats[index] = viewMatrix[index].toFloat()
        val projectionFloats = projectionMatrixBuffer.get()
        for (index in projectionMatrix.indices) projectionFloats[index] = projectionMatrix[index].toFloat()
        Native.vgcode_render(pointer, viewFloats, projectionFloats)
    }

    fun setThemeColors() {
        setColors(
            ThemesRepo.getColor(R.attr.gcodeViewerSkirt),
            ThemesRepo.getColor(R.attr.gcodeViewerExternalPerimeter),
            ThemesRepo.getColor(R.attr.gcodeViewerSupportMaterial),
            ThemesRepo.getColor(R.attr.gcodeViewerSupportMaterialInterface),
            ThemesRepo.getColor(R.attr.gcodeViewerInternalInfill),
            ThemesRepo.getColor(R.attr.gcodeViewerSolidInfill),
            ThemesRepo.getColor(R.attr.gcodeViewerWipeTower),
        )
    }

    fun setColors(
        skirt: Int,
        externalPerimeter: Int,
        supportMaterial: Int,
        supportMaterialInterface: Int,
        internalInfill: Int,
        solidInfill: Int,
        wipeTower: Int,
    ) {
        Native.vgcode_set_colors(
            pointer,
            intArrayOf(
                Color.red(skirt), Color.green(skirt), Color.blue(skirt),
                Color.red(externalPerimeter), Color.green(externalPerimeter), Color.blue(externalPerimeter),
                Color.red(supportMaterial), Color.green(supportMaterial), Color.blue(supportMaterial),
                Color.red(supportMaterialInterface), Color.green(supportMaterialInterface), Color.blue(supportMaterialInterface),
                Color.red(internalInfill), Color.green(internalInfill), Color.blue(internalInfill),
                Color.red(solidInfill), Color.green(solidInfill), Color.blue(solidInfill),
                Color.red(wipeTower), Color.green(wipeTower), Color.blue(wipeTower),
            ),
        )
    }

    fun reset() = Native.vgcode_reset(pointer)

    fun release() {
        if (pointer != 0L) {
            Native.vgcode_release(pointer)
            pointer = 0L
        }
    }

    @Suppress("DEPRECATION")
    @Throws(Throwable::class)
    protected open fun finalize() {
        release()
    }

    companion object {
        const val EXTRUSION_ROLE_NONE = 0
        const val EXTRUSION_ROLE_PERIMETER = 1
        const val EXTRUSION_ROLE_EXTERNAL_PERIMETER = 2
        const val EXTRUSION_ROLE_OVERHANG_PERIMETER = 3
        const val EXTRUSION_ROLE_INTERNAL_INFILL = 4
        const val EXTRUSION_ROLE_SOLID_INFILL = 5
        const val EXTRUSION_ROLE_TOP_SOLID_INFILL = 6
        const val EXTRUSION_ROLE_IRONING = 7
        const val EXTRUSION_ROLE_BRIDGE_INFILL = 8
        const val EXTRUSION_ROLE_GAP_FILL = 9
        const val EXTRUSION_ROLE_SKIRT = 10
        const val EXTRUSION_ROLE_SUPPORT_MATERIAL = 11
        const val EXTRUSION_ROLE_SUPPORT_MATERIAL_INTERFACE = 12
        const val EXTRUSION_ROLE_WIPE_TOWER = 13
        const val EXTRUSION_ROLE_CUSTOM = 14
        const val EXTRUSION_ROLES_COUNT = 15

        // gcode preview color modes (see libvgcode EViewType)
        const val VIEW_TYPE_FEATURE = 0
        const val VIEW_TYPE_TOOL = 11

        // Option types (see libvgcode EOptionType)
        const val OPTION_TYPE_SEAMS = 4
    }
}
