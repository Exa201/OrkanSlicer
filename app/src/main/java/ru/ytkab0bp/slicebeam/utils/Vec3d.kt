package ru.ytkab0bp.slicebeam.utils

/** Mutable three-component double vector shared by model geometry and renderer code. */
class Vec3d {
    @JvmField var x: Double = 0.0
    @JvmField var y: Double = 0.0
    @JvmField var z: Double = 0.0

    constructor()

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(from: Vec3d) : this(from.x, from.y, from.z)

    fun center(with: Vec3d): Vec3d = clone().add(with.clone().add(clone().negate()).multiply(0.5))

    fun set(vec: Vec3d): Vec3d {
        x = vec.x
        y = vec.y
        z = vec.z
        return this
    }

    fun set(x: Double, y: Double, z: Double): Vec3d {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun add(vec: Vec3d): Vec3d {
        x += vec.x
        y += vec.y
        z += vec.z
        return this
    }

    fun add(x: Double, y: Double, z: Double): Vec3d {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun multiply(vec: Vec3d): Vec3d {
        x *= vec.x
        y *= vec.y
        z *= vec.z
        return this
    }

    fun multiply(x: Double, y: Double, z: Double): Vec3d {
        this.x *= x
        this.y *= y
        this.z *= z
        return this
    }

    fun multiply(m: Double): Vec3d {
        x *= m
        y *= m
        z *= m
        return this
    }

    fun magnitude(): Double = kotlin.math.sqrt(x * x + y * y + z * z)

    fun distance(with: Vec3d): Double {
        val dx = x - with.x
        val dy = with.y - y
        val dz = with.z - z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun normalize(): Vec3d {
        val magnitude = magnitude()
        if (magnitude > 0) multiply(1 / magnitude)
        return this
    }

    fun negate(): Vec3d = multiply(-1.0)

    fun divide(vec: Vec3d): Vec3d {
        x /= vec.x
        y /= vec.y
        z /= vec.z
        return this
    }

    fun crossProduct(with: Vec3d): Vec3d = Vec3d(
        y * with.z - z * with.y,
        z * with.x - x * with.z,
        x * with.y - y * with.x
    )

    fun clone(): Vec3d = Vec3d(this)

    fun asArray(): FloatArray = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat(), 1f)

    fun asDoubleArray(): DoubleArray = doubleArrayOf(x, y, z, 1.0)
}
