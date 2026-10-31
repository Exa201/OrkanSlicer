package ru.ytkab0bp.slicebeam

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer
import java.io.File

/** Regression coverage for Orca's pause-marker move-index correction in the native preview. */
@RunWith(AndroidJUnit4::class)
class GCodePauseLayerPreviewDeviceTest {
    @Test
    fun pauseAfterTimeFlushRemainsOnItsOwnLayerAndPreviewLayersStayOrdered() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "pause-layer-preview-regression.gcode")
        val result = GCodeProcessorResult(file.also { it.writeText(gcodeWithPauseOnSecondLayer()) })
        val viewer = GCodeViewer()
        val egl = EglContext()
        try {
            egl.makeCurrent()
            viewer.initGL()
            viewer.load(result)

            assertEquals("The pause must not split or reorder the three printed layers", 3L, viewer.getLayersCount())
        } finally {
            viewer.release()
            egl.close()
            result.release()
            file.delete()
        }
    }

    private fun gcodeWithPauseOnSecondLayer(): String = buildString {
        appendLine("M83")
        appendLine("G90")
        for (layer in 1..3) {
            appendLine("; CHANGE_LAYER")
            appendLine("; LAYER_HEIGHT: 0.2")
            if (layer == 2) {
                appendLine("G1 E-0.8 F3000")
                appendLine("; PAUSE_PRINTING")
                appendLine("M601")
            }
            appendLine("G1 X10 Y10 F12000")
            appendLine("G1 Z${0.2 * layer}")
            if (layer == 2) appendLine("G1 E0.8 F3000")
            appendLine("; FEATURE: Outer wall")
            appendLine("G1 X50 Y10 E2 F3000")
            appendLine("G1 X50 Y50 E2")
            appendLine("G1 X10 Y50 E2")
            appendLine("G1 X10 Y10 E2")
        }
    }

    private class EglContext : AutoCloseable {
        private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        private val surface: EGLSurface
        private val context: android.opengl.EGLContext

        init {
            assertTrue("Unable to acquire EGL display", display != EGL14.EGL_NO_DISPLAY)
            val version = IntArray(2)
            assertTrue("Unable to initialize EGL", EGL14.eglInitialize(display, version, 0, version, 1))

            val configAttributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, 0x40, // EGL_OPENGL_ES3_BIT_KHR
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            assertTrue("No GLES3 EGL config available", EGL14.eglChooseConfig(
                display, configAttributes, 0, configs, 0, configs.size, configCount, 0,
            ))
            assertNotNull(configs[0])
            val config = requireNotNull(configs[0])
            val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
            assertTrue("Unable to create GLES3 context", context != EGL14.EGL_NO_CONTEXT)
            val surfaceAttributes = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            surface = EGL14.eglCreatePbufferSurface(display, config, surfaceAttributes, 0)
            assertTrue("Unable to create EGL pbuffer", surface != EGL14.EGL_NO_SURFACE)
        }

        fun makeCurrent() {
            assertTrue("Unable to activate GLES3 context", EGL14.eglMakeCurrent(display, surface, surface, context))
        }

        override fun close() {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
    }
}
