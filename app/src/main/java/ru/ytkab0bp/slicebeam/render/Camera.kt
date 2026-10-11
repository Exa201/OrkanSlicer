package ru.ytkab0bp.slicebeam.render

import androidx.core.math.MathUtils
import ru.ytkab0bp.slicebeam.utils.DoubleMatrix
import ru.ytkab0bp.slicebeam.utils.Vec3d

class Camera {
    private val viewMatrix = DoubleArray(16)
    private var viewMatrixDirty = true

    @JvmField var position = Vec3d(0.0, 0.0, 0.0)
    @JvmField var origin = Vec3d(0.0, 0.0, 0.0)
    @JvmField var up = Vec3d(0.0, 0.0, 1.0)

    private val tempMatrix = DoubleArray(16)
    var zoom: Float = 1f

    // Reuse temporary vectors during gesture calculations to avoid per-event allocations.
    private val scratchDir = Vec3d()
    private val scratchUpMod = Vec3d()
    private val scratchRight = Vec3d()
    private val scratchScreenY = Vec3d()
    private val rotBuf = DoubleArray(4)

    fun invalidate() {
        viewMatrixDirty = true
    }

    fun getDirToBed(): Vec3d {
        // origin*(1,1,0) + (-position) = (origin.x-position.x, origin.y-position.y, -position.z)
        scratchDir.x = origin.x - position.x
        scratchDir.y = origin.y - position.y
        scratchDir.z = -position.z
        return scratchDir.normalize()
    }

    fun getDirForward(): Vec3d {
        scratchDir.x = origin.x - position.x
        scratchDir.y = origin.y - position.y
        scratchDir.z = origin.z - position.z
        return scratchDir.normalize()
    }

    fun getViewModelMatrix(): DoubleArray {
        if (viewMatrixDirty) {
            DoubleMatrix.setLookAtM(
                viewMatrix, 0,
                position.x, position.y, position.z,
                origin.x, origin.y, origin.z,
                up.x, up.y, up.z
            )
            viewMatrixDirty = false
        }
        return viewMatrix
    }

    fun zoom(zoom: Float) {
        this.zoom = MathUtils.clamp(this.zoom + zoom / 25f, 1f, 10f)
    }

    fun calcScreenMovement(x: Float, y: Float): Vec3d {
        val scaledX = x / zoom
        val scaledY = y / zoom
        computeScreenBasis()
        return Vec3d(
            scratchRight.x * scaledX + scratchScreenY.x * scaledY,
            scratchRight.y * scaledX + scratchScreenY.y * scaledY,
            scratchRight.z * scaledX + scratchScreenY.z * scaledY
        )
    }

    // Computes scratchRight and scratchScreenY from current camera direction without allocating.
    private fun computeScreenBasis() {
        scratchDir.x = origin.x - position.x
        scratchDir.y = origin.y - position.y
        scratchDir.z = origin.z - position.z
        scratchDir.normalize()

        val yaw = Math.atan2(scratchDir.x, scratchDir.y)
        val pitch = Math.asin(-scratchDir.z)
        val sinPitch = Math.sin(pitch)

        scratchUpMod.x = sinPitch * Math.sin(yaw)
        scratchUpMod.y = sinPitch * Math.cos(yaw)
        scratchUpMod.z = Math.cos(pitch)

        // right = dir × upMod
        scratchRight.x = scratchDir.y * scratchUpMod.z - scratchDir.z * scratchUpMod.y
        scratchRight.y = scratchDir.z * scratchUpMod.x - scratchDir.x * scratchUpMod.z
        scratchRight.z = scratchDir.x * scratchUpMod.y - scratchDir.y * scratchUpMod.x

        // screenY = dir × right
        scratchScreenY.x = scratchDir.y * scratchRight.z - scratchDir.z * scratchRight.y
        scratchScreenY.y = scratchDir.z * scratchRight.x - scratchDir.x * scratchRight.z
        scratchScreenY.z = scratchDir.x * scratchRight.y - scratchDir.y * scratchRight.x
    }

    fun move(x: Float, y: Float) {
        val scaledX = x / zoom
        val scaledY = y / zoom
        computeScreenBasis()

        val mx = scratchRight.x * scaledX + scratchScreenY.x * scaledY
        val my = scratchRight.y * scaledX + scratchScreenY.y * scaledY
        val mz = scratchRight.z * scaledX + scratchScreenY.z * scaledY

        position.x += mx
        position.y += my
        position.z += mz
        origin.x += mx
        origin.y += my
        origin.z += mz
        viewMatrixDirty = true
    }

    fun rotateAround(rx: Double, ry: Double) {
        rotBuf[0] = position.x - origin.x
        rotBuf[1] = position.y - origin.y
        rotBuf[2] = position.z - origin.z
        rotBuf[3] = 1.0

        DoubleMatrix.setIdentityM(tempMatrix, 0)

        scratchDir.x = origin.x - position.x
        scratchDir.y = origin.y - position.y
        scratchDir.z = origin.z - position.z
        scratchDir.normalize()

        val yaw = Math.atan2(scratchDir.x, scratchDir.y)
        val pitch = Math.toDegrees(Math.asin(-scratchDir.z))

        var mry = -ry
        if (pitch + mry > 90) {
            mry = 0.0
        } else if (pitch + mry < -90) {
            mry = 0.0
        }

        DoubleMatrix.rotateM(tempMatrix, 0, -mry * Math.cos(yaw), 1.0, 0.0, 0.0)
        DoubleMatrix.rotateM(tempMatrix, 0, mry * Math.sin(yaw), 0.0, 1.0, 0.0)
        DoubleMatrix.rotateM(tempMatrix, 0, rx, 0.0, 0.0, 1.0)

        DoubleMatrix.multiplyMV(rotBuf, 0, tempMatrix, 0, rotBuf, 0)
        position.x = rotBuf[0] / rotBuf[3] + origin.x
        position.y = rotBuf[1] / rotBuf[3] + origin.y
        position.z = rotBuf[2] / rotBuf[3] + origin.z
        viewMatrixDirty = true
    }
}
