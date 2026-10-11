package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.GLShaderProgram
import ru.ytkab0bp.slicebeam.slic3r.GLShadersManager
import ru.ytkab0bp.slicebeam.slic3r.Slic3rUtils
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.DoubleMatrix
import ru.ytkab0bp.slicebeam.utils.Vec3d
import kotlin.math.min

class CoordAxes {
    @JvmField var origin = Vec3d(0.0, 0.0, 0.0)

    private var stemRadius = 0.5f
    private var stemLength = 25.0f
    private var tipRadius = 2.5f * stemRadius
    private var tipLength = 5.0f

    private val arrow = GLModel()

    fun setStemLength(stemLength: Float) {
        this.stemLength = stemLength
        arrow.reset()
    }

    private val matrix = DoubleArray(16)
    private val matrix2 = DoubleArray(16)
    private val matrix3 = DoubleArray(16)
    private val normals = DoubleArray(12)

    private fun renderAxis(shader: GLShaderProgram, viewMatrix: DoubleArray, projectionMatrix: DoubleArray) {
        DoubleMatrix.multiplyMM(matrix3, 0, viewMatrix, 0, matrix2, 0)
        shader.setUniformMatrix4fv("view_model_matrix", matrix3)
        shader.setUniformMatrix4fv("projection_matrix", projectionMatrix)
        Slic3rUtils.calcViewNormalMatrix(viewMatrix, matrix2, normals)
        shader.setUniformMatrix3fv("view_normal_matrix", normals)
        arrow.render()
    }

    fun render(
        shadersManager: GLShadersManager,
        viewMatrix: DoubleArray,
        projectionMatrix: DoubleArray,
        emissionFactor: Float,
        invZoom: Float
    ) {
        if (!arrow.isInitialized()) {
            arrow.stilizedArrow(tipRadius, tipLength, stemRadius, stemLength)
        }

        val currentShader = shadersManager.getCurrentShader()
        val shader = shadersManager.get(GLShadersManager.SHADER_GOURAUD_LIGHT)
        if (currentShader != null) {
            currentShader.stopUsing()
        }

        shader.startUsing()
        shader.setUniform("emission_factor", emissionFactor)

        DoubleMatrix.setIdentityM(matrix, 0)
        val scale = min(1f, invZoom * 2f)
        DoubleMatrix.scaleM(matrix, 0, scale.toDouble(), scale.toDouble(), scale.toDouble())

        arrow.setColor(ThemesRepo.getColor(R.attr.xTrackColor))
        DoubleMatrix.setIdentityM(matrix2, 0)
        DoubleMatrix.translateM(matrix2, 0, origin.x, origin.y, origin.z)
        DoubleMatrix.rotateM(matrix2, 0, 90.0, 0.0, 1.0, 0.0)
        DoubleMatrix.multiplyMM(matrix2, 0, matrix, 0, matrix2, 0)
        renderAxis(shader, viewMatrix, projectionMatrix)

        arrow.setColor(ThemesRepo.getColor(R.attr.yTrackColor))
        DoubleMatrix.setIdentityM(matrix2, 0)
        DoubleMatrix.translateM(matrix2, 0, origin.x, origin.y, origin.z)
        DoubleMatrix.rotateM(matrix2, 0, -90.0, 1.0, 0.0, 0.0)
        DoubleMatrix.multiplyMM(matrix2, 0, matrix, 0, matrix2, 0)
        renderAxis(shader, viewMatrix, projectionMatrix)

        arrow.setColor(ThemesRepo.getColor(R.attr.zTrackColor))
        DoubleMatrix.setIdentityM(matrix2, 0)
        DoubleMatrix.translateM(matrix2, 0, origin.x, origin.y, origin.z)
        DoubleMatrix.multiplyMM(matrix2, 0, matrix, 0, matrix2, 0)
        renderAxis(shader, viewMatrix, projectionMatrix)

        shader.stopUsing()
        if (currentShader != null) {
            currentShader.startUsing()
        }
    }

    fun release() {
        arrow.release()
    }
}
