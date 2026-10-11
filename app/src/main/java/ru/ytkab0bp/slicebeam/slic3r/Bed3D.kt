package ru.ytkab0bp.slicebeam.slic3r

import android.opengl.GLES30
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.render.CoordAxes
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.DebugUtils
import ru.ytkab0bp.slicebeam.utils.DoubleMatrix
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File

open class Bed3D {
    private var pointer: Long = 0L
    private val triangles: GLModel
    private val gridlines: GLModel
    private val contourlines: GLModel
    private val axes = CoordAxes()
    private var boundingVolume: DoubleArray? = null
    private var min: Vec3d? = null
    private var max: Vec3d? = null
    private var likelyDelta: Boolean = false
    private val modelMatrix = DoubleArray(16)
    private val outModelMatrix = DoubleArray(16)

    init {
        val data = LongArray(3)
        pointer = Native.bed_create(data)
        triangles = GLModel(data[0])
        gridlines = GLModel(data[1])
        contourlines = GLModel(data[2])
    }

    open fun configure(file: File) {
        configure(file.absolutePath)
    }

    private fun configure(path: String) {
        Native.bed_configure(pointer, path)
        Native.bed_init_triangles_mesh(pointer, triangles.pointer)
        boundingVolume = Native.bed_get_bounding_volume(pointer)

        min = null
        max = null

        axes.origin.set(0.0, 0.0, GROUND_Z.toDouble())
        axes.setStemLength(0.1f * Native.bed_get_bounding_volume_max_size(pointer))

        if (isValid()) {
            val volumeMin = requireNotNull(getVolumeMin())
            val volumeMax = requireNotNull(getVolumeMax())
            val center = volumeMin.center(volumeMax)
            likelyDelta = (center.x == 0.0 || center.y == 0.0) && volumeMin.x < 0 && volumeMin.y < 0
        } else {
            likelyDelta = false
        }
    }

    open fun arrange(model: Model): Boolean = Native.bed_arrange(pointer, model.pointer)

    open fun getVolumeMin(): Vec3d? {
        if (min == null && boundingVolume != null) {
            val volume = requireNotNull(boundingVolume)
            min = Vec3d(volume[0], volume[1], volume[2])
        }
        return min
    }

    open fun getVolumeMax(): Vec3d? {
        if (max == null && boundingVolume != null) {
            val volume = requireNotNull(boundingVolume)
            max = Vec3d(volume[3], volume[4], volume[5])
        }
        return max
    }

    open fun isValid(): Boolean = boundingVolume != null

    open val raycaster: GLModel.MeshRaycaster
        get() = triangles.raycaster

    open fun render(
        shadersManager: GLShadersManager,
        bottom: Boolean,
        viewModelMatrix: DoubleArray,
        projectionMatrix: DoubleArray,
        invZoom: Float,
    ) {
        render(shadersManager, bottom, viewModelMatrix, projectionMatrix, invZoom, true)
    }

    open fun render(
        shadersManager: GLShadersManager,
        bottom: Boolean,
        viewModelMatrix: DoubleArray,
        projectionMatrix: DoubleArray,
        invZoom: Float,
        isActive: Boolean,
    ) {
        DebugUtils.assertTrue(viewModelMatrix.size == 16)
        DebugUtils.assertTrue(projectionMatrix.size == 16)

        DoubleMatrix.setIdentityM(modelMatrix, 0)
        if (!likelyDelta) {
            val volumeMin = requireNotNull(getVolumeMin())
            DoubleMatrix.translateM(
                modelMatrix,
                0,
                -volumeMin.x * 2,
                -volumeMin.y * 2,
                -volumeMin.z,
            )
        }
        DoubleMatrix.multiplyMM(outModelMatrix, 0, viewModelMatrix, 0, modelMatrix, 0)
        renderDefaultBed(shadersManager, bottom, outModelMatrix, projectionMatrix, isActive)
        if (isActive) axes.render(shadersManager, viewModelMatrix, projectionMatrix, 0.25f, invZoom)
    }

    private fun renderDefaultBed(
        shadersManager: GLShadersManager,
        bottom: Boolean,
        viewModelMatrix: DoubleArray,
        projectionMatrix: DoubleArray,
        isActive: Boolean,
    ) {
        val shader = shadersManager.get(GLShadersManager.SHADER_FLAT)
        shader.startUsing()

        shader.setUniformMatrix4fv("view_model_matrix", viewModelMatrix)
        shader.setUniformMatrix4fv("projection_matrix", projectionMatrix)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        if (!bottom) {
            GLES30.glDepthMask(false)
            var bedColor = ThemesRepo.getColor(R.attr.defaultBedColor)
            if (!isActive) bedColor = ColorUtils.blendARGB(bedColor, 0xFF444444.toInt(), 0.5f)
            triangles.setColor(bedColor)
            triangles.render()
            GLES30.glDepthMask(true)
        }

        GLES30.glLineWidth(ViewUtils.dp(1).toFloat())
        var gridColor = ThemesRepo.getColor(R.attr.bedGridlinesColor)
        if (!isActive) gridColor = ColorUtils.blendARGB(gridColor, 0xFF444444.toInt(), 0.4f)
        gridlines.setColor(gridColor)
        gridlines.render()

        var contourColor = ThemesRepo.getColor(R.attr.bedContourlinesColor)
        if (!isActive) contourColor = ColorUtils.blendARGB(contourColor, 0xFF444444.toInt(), 0.4f)
        contourlines.setColor(contourColor)
        contourlines.render()

        GLES30.glDisable(GLES30.GL_BLEND)
        shader.stopUsing()
    }

    private fun renderTexturedBed(
        shadersManager: GLShadersManager,
        bottom: Boolean,
        viewModelMatrix: FloatArray,
        projectionMatrix: FloatArray,
    ) {
        val shader = shadersManager.get(GLShadersManager.SHADER_PRINTBED)
        shader.startUsing()

        shader.setUniform3f("view_model_matrix", *viewModelMatrix)
        shader.setUniform3f("projection_matrix", *projectionMatrix)
        shader.setUniform("transparent_background", bottom)
        shader.setUniform("svg_source", false)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        if (bottom) GLES30.glDepthMask(false)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        if (bottom) GLES30.glFrontFace(GLES30.GL_CW)
        // TODO: GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        if (bottom) GLES30.glFrontFace(GLES30.GL_CCW)

        GLES30.glDisable(GLES30.GL_BLEND)
        if (bottom) GLES30.glDepthMask(true)
        shader.stopUsing()
    }

    open fun release() {
        if (pointer != 0L) {
            Native.bed_release(pointer)
            axes.release()
            pointer = 0L
            // The triangle/grid/contour meshes are owned by the native bed and released there.
        }
    }

    @Suppress("DEPRECATION")
    @Throws(Throwable::class)
    protected open fun finalize() {
        release()
    }

    companion object {
        private const val GROUND_Z = -0.02f
    }
}
