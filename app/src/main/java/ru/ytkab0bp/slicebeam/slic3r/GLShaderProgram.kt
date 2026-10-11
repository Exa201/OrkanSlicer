package ru.ytkab0bp.slicebeam.slic3r

import android.content.res.AssetManager
import android.graphics.Color
import android.opengl.GLES30
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.DebugUtils
import ru.ytkab0bp.slicebeam.utils.IOUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class GLShaderProgram(name: String) {
    @JvmField
    var pointer: Long = 0

    init {
        val assets: AssetManager = SliceBeam.requireInstance().assets
        try {
            val fragment = assets.open("shaders/$name.fs").use { IOUtils.readString(it) }
            val vertex = assets.open("shaders/$name.vs").use { IOUtils.readString(it) }
            pointer = Native.shader_init_from_texts(name, fragment, vertex)
        } catch (error: IOException) {
            throw RuntimeException(error)
        }
    }

    open fun startUsing() {
        Native.shader_start_using(pointer)
    }

    open fun stopUsing() {
        Native.shader_stop_using(pointer)
    }

    open fun getUniformLocation(name: String): Int {
        // This function uses the native uniform cache. Java one does not.
        return Native.shader_get_uniform_location(pointer, name)
    }

    open fun getAttribLocation(name: String): Int {
        // Same as getUniformLocation.
        return Native.shader_get_attrib_location(pointer, name)
    }

    open fun setUniform(name: String, value: Boolean) {
        GLES30.glUniform1i(getUniformLocation(name), if (value) 1 else 0)
    }

    open fun setUniform(name: String, value: Float) {
        GLES30.glUniform1f(getUniformLocation(name), value)
    }

    open fun setUniformMatrix3fv(name: String, value: DoubleArray) {
        DebugUtils.assertTrue(value.size == 12)

        val floats = float12Buffer.get()!!
        for (i in value.indices) {
            floats[i] = value[i].toFloat()
        }
        setUniformMatrix3fv(name, floats)
    }

    open fun setUniformMatrix3fv(name: String, value: FloatArray) {
        DebugUtils.assertTrue(value.size == 12)

        val buffer = matrixBuffer.get()!!
        buffer.position(0).limit(12)
        buffer.put(value)
        buffer.flip()
        GLES30.glUniformMatrix3fv(getUniformLocation(name), 1, false, buffer)
    }

    open fun setUniformMatrix4fv(name: String, value: DoubleArray) {
        DebugUtils.assertTrue(value.size == 16)

        val floats = float16Buffer.get()!!
        for (i in value.indices) {
            floats[i] = value[i].toFloat()
        }
        setUniformMatrix4fv(name, floats)
    }

    open fun setUniformMatrix4fv(name: String, value: FloatArray) {
        DebugUtils.assertTrue(value.size == 16)

        val buffer = matrixBuffer.get()!!
        buffer.position(0).limit(16)
        buffer.put(value)
        buffer.flip()
        GLES30.glUniformMatrix4fv(getUniformLocation(name), 1, false, buffer)
    }

    open fun setUniformColor(name: String, color: Int) {
        setUniform4f(
            name,
            Color.red(color).toFloat() / 0xFF,
            Color.green(color).toFloat() / 0xFF,
            Color.blue(color).toFloat() / 0xFF,
            Color.alpha(color).toFloat() / 0xFF
        )
    }

    open fun setUniform4f(name: String, vararg value: Float) {
        DebugUtils.assertTrue(value.size == 4)
        GLES30.glUniform4f(getUniformLocation(name), value[0], value[1], value[2], value[3])
    }

    open fun setUniform3f(name: String, vararg value: Float) {
        DebugUtils.assertTrue(value.size == 3)
        GLES30.glUniform3f(getUniformLocation(name), value[0], value[1], value[2])
    }

    open fun setUniform2f(name: String, vararg value: Float) {
        DebugUtils.assertTrue(value.size == 2)
        GLES30.glUniform2f(getUniformLocation(name), value[0], value[1])
    }

    open fun getId(): Int = Native.shader_get_id(pointer)

    open fun release() {
        if (pointer != 0L) {
            Native.shader_release(pointer)
            pointer = 0L
        }
    }

    @Suppress("deprecation")
    @Throws(Throwable::class)
    protected open fun finalize() {
        release()
    }

    companion object {
        private val matrixBuffer = object : ThreadLocal<FloatBuffer>() {
            override fun initialValue(): FloatBuffer = ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }

        private val float16Buffer = object : ThreadLocal<FloatArray>() {
            override fun initialValue(): FloatArray = FloatArray(16)
        }

        private val float12Buffer = object : ThreadLocal<FloatArray>() {
            override fun initialValue(): FloatArray = FloatArray(12)
        }
    }
}
