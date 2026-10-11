package ru.ytkab0bp.slicebeam.slic3r

import android.opengl.GLES30
import androidx.annotation.Keep
import androidx.annotation.StringDef

open class GLShadersManager {
    private val shaders = HashMap<String, GLShaderProgram>()

    init {
        managers.add(this)
    }

    open fun clearShaders() {
        shaders.values.forEach(GLShaderProgram::release)
        shaders.clear()
        managers.remove(this)
    }

    open fun get(@ShaderType key: String): GLShaderProgram =
        shaders.getOrPut(key) { GLShaderProgram(key) }

    open fun getCurrentShader(): GLShaderProgram? {
        val idRef = intArrayOf(0)
        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, idRef, 0)
        val id = idRef[0]
        if (id != 0) {
            for (program in shaders.values) {
                if (program.getId() == id) return program
            }
        }
        return null
    }

    companion object {
        const val SHADER_BACKGROUND = "background"
        const val SHADER_DASHED_LINES = "dashed_lines"
        const val SHADER_FLAT = "flat"
        const val SHADER_FLAT_CLIP = "flat_clip"
        const val SHADER_FLAT_TEXTURE = "flat_texture"
        const val SHADER_GOURAUD = "gouraud"
        const val SHADER_GOURAUD_LIGHT = "gouraud_light"
        const val SHADER_GOURAUD_LIGHT_INSTANCED = "gouraud_light_instanced"
        const val SHADER_IMGUI = "imgui"
        const val SHADER_MM_CONTOUR = "mm_contour"
        const val SHADER_MM_GOURAUD = "mm_gouraud"
        const val SHADER_PRINTBED = "printbed"
        const val SHADER_TOOLPATHS_COG = "toolpaths_cog"
        const val SHADER_VARIABLE_LAYER_HEIGHT = "variable_layer_height"
        const val SHADER_WIREFRAME = "wireframe"
        const val SHADER_BEAM_INTRO = "beam_intro"

        @StringDef(
            value = [
                SHADER_BACKGROUND,
                SHADER_DASHED_LINES,
                SHADER_FLAT,
                SHADER_FLAT_CLIP,
                SHADER_FLAT_TEXTURE,
                SHADER_GOURAUD,
                SHADER_GOURAUD_LIGHT,
                SHADER_GOURAUD_LIGHT_INSTANCED,
                SHADER_IMGUI,
                SHADER_MM_CONTOUR,
                SHADER_MM_GOURAUD,
                SHADER_GOURAUD,
                SHADER_PRINTBED,
                SHADER_TOOLPATHS_COG,
                SHADER_VARIABLE_LAYER_HEIGHT,
                SHADER_WIREFRAME,
                SHADER_BEAM_INTRO
            ]
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class ShaderType

        private val managers = ArrayList<GLShadersManager>()

        @JvmStatic
        @Keep
        private fun getCurrentShaderPointer(): Long {
            for (manager in managers) {
                val program = manager.getCurrentShader()
                if (program != null) return program.pointer
            }
            return 0L
        }
    }
}
