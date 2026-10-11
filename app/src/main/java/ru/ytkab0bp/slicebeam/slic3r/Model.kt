package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.UUID

/** Java-compatible wrapper for an Orca model owned by the native slicer. */
open class Model private constructor(@JvmField var pointer: Long) {
    @JvmField
    val key: String = UUID.randomUUID().toString()

    @Volatile
    private var geometryRevisionValue: Long = 0L

    private var boundingExact: DoubleArray? = null
    private var boundingApprox: DoubleArray? = null

    constructor() : this(Native.model_create())

    @Throws(Slic3rRuntimeError::class)
    constructor(file: File) : this(file.absolutePath, 0)

    @Throws(Slic3rRuntimeError::class)
    constructor(file: File, plateId: Int) : this(file.absolutePath, plateId)

    @Throws(Slic3rRuntimeError::class)
    constructor(path: String) : this(readModelPointer(path, 0))

    @Throws(Slic3rRuntimeError::class)
    constructor(path: String, plateId: Int) : this(readModelPointer(path, plateId))

    open fun getBoundingBoxExact(i: Int, min: Vec3d, max: Vec3d) {
        val data = Native.model_get_bounding_box_exact(pointer, i)
        min.set(data[0], data[1], data[2])
        max.set(data[3], data[4], data[5])
    }

    open fun getBoundingBoxApprox(i: Int, min: Vec3d, max: Vec3d) {
        val data = Native.model_get_bounding_box_approx(pointer, i)
        min.set(data[0], data[1], data[2])
        max.set(data[3], data[4], data[5])
    }

    open val boundingBoxExactMin: Vec3d
        get() {
        val bounds = boundingExact ?: Native.model_get_bounding_box_exact_global(pointer).also {
            boundingExact = it
        }
        return Vec3d(bounds[0], bounds[1], bounds[2])
    }

    open val boundingBoxExactMax: Vec3d
        get() {
        val bounds = boundingExact ?: Native.model_get_bounding_box_exact_global(pointer).also {
            boundingExact = it
        }
        return Vec3d(bounds[3], bounds[4], bounds[5])
    }

    open val boundingBoxApproxMin: Vec3d
        get() {
        val bounds = boundingApprox ?: Native.model_get_bounding_box_approx_global(pointer).also {
            boundingApprox = it
        }
        return Vec3d(bounds[0], bounds[1], bounds[2])
    }

    open val boundingBoxApproxMax: Vec3d
        get() {
        val bounds = boundingApprox ?: Native.model_get_bounding_box_approx_global(pointer).also {
            boundingApprox = it
        }
        return Vec3d(bounds[3], bounds[4], bounds[5])
    }

    open fun resetBoundingBox() {
        boundingExact = null
        boundingApprox = null
        geometryRevisionValue++
    }

    /** Revision for invalidating inexpensive analysis results tied to this model geometry. */
    open val geometryRevision: Long
        get() = geometryRevisionValue

    open fun translate(i: Int, x: Double, y: Double, z: Double) {
        Native.model_translate(pointer, i, x, y, z)
        resetBoundingBox()
    }

    open fun translate(x: Double, y: Double, z: Double) {
        Native.model_translate_global(pointer, x, y, z)
        resetBoundingBox()
    }

    open fun ensureOnBed(i: Int) {
        Native.model_ensure_on_bed(pointer, i)
        resetBoundingBox()
    }

    open fun scale(i: Int, x: Double, y: Double, z: Double) {
        Native.model_scale(pointer, i, x, y, z)
        resetBoundingBox()
    }

    open fun rotate(i: Int, x: Double, y: Double, z: Double) {
        Native.model_rotate(pointer, i, x, y, z)
        resetBoundingBox()
    }

    open fun flattenRotate(i: Int, surface: GLModel) {
        Native.model_flatten_rotate(pointer, i, surface.pointer)
        resetBoundingBox()
    }

    open fun getPointer(): Long = pointer

    open val objectsCount: Int
        get() {
            if (pointer == 0L) {
                android.util.Log.e("Model", "CRITICAL ERROR: getObjectsCount called with pointer == 0!", Exception())
                return 0
            }
            return Native.model_get_objects_count(pointer)
        }

    open fun getObjectName(i: Int): String? {
        if (pointer == 0L || i < 0 || i >= objectsCount) return null
        val name = Native.model_get_object_name(pointer, i)
        if (name.isNullOrEmpty()) return null
        val trimmed = name.trim { it <= ' ' }
        return trimmed.takeIf { it.isNotEmpty() }
    }

    open fun getTriangleCount(i: Int): Int =
        if (pointer == 0L) 0 else Native.model_get_triangle_count(pointer, i)

    open fun simplify(i: Int, keepRatio: Float): Int {
        val result = Native.model_simplify(pointer, i, keepRatio)
        resetBoundingBox()
        return result
    }

    open fun addObject(from: Model, i: Int) {
        Native.model_add_object_from_another(pointer, from.pointer, i)
        resetBoundingBox()
    }

    open fun deleteObject(i: Int) {
        Native.model_delete_object(pointer, i)
        resetBoundingBox()
    }

    /** Split object i into separate objects; returns the piece count, or 0 if nothing to split. */
    open fun split(i: Int): Int {
        val result = Native.model_split(pointer, i)
        if (result > 0) resetBoundingBox()
        return result
    }

    open fun cut(
        objectIndex: Int,
        zHeight: Double,
        rotX: Double,
        rotY: Double,
        keepUpper: Boolean,
        keepLower: Boolean,
    ): Boolean {
        val result = Native.model_cut(pointer, objectIndex, zHeight, rotX, rotY, keepUpper, keepLower)
        if (result) resetBoundingBox()
        return result
    }

    open fun addConnector(
        objectIndex: Int,
        position: Vec3d,
        radius: Float,
        height: Float,
        type: Int,
        rotX: Double,
        rotY: Double,
    ) {
        Native.model_add_connector_on_plane(
            pointer, objectIndex, position.x, position.y, position.z, radius, height, type, rotX, rotY,
        )
        resetBoundingBox()
    }

    open fun addConnector(objectIndex: Int, position: Vec3d, radius: Float, height: Float, type: Int) {
        Native.model_add_connector(pointer, objectIndex, position.x, position.y, position.z, radius, height, type)
        resetBoundingBox()
    }

    open fun removeConnector(objectIndex: Int, connectorIndex: Int) {
        Native.model_remove_connector(pointer, objectIndex, connectorIndex)
        resetBoundingBox()
    }

    open fun clearConnectors(objectIndex: Int) {
        Native.model_clear_connectors(pointer, objectIndex)
        resetBoundingBox()
    }

    open fun getTranslation(i: Int, vec: Vec3d) {
        val translation = Native.model_get_translation(pointer, i)
        vec.set(translation[0], translation[1], translation[2])
    }

    open fun getRotation(i: Int, vec: Vec3d) {
        val rotation = Native.model_get_rotation(pointer, i)
        vec.set(rotation[0], rotation[1], rotation[2])
    }

    open fun isLeftHanded(i: Int): Boolean = Native.model_is_left_handed(pointer, i)

    open fun getScale(i: Int, vec: Vec3d) {
        val scale = Native.model_get_scale(pointer, i)
        vec.set(scale[0], scale[1], scale[2])
    }

    open fun getMirror(i: Int, vec: Vec3d) {
        val mirror = Native.model_get_mirror(pointer, i)
        vec.set(mirror[0], mirror[1], mirror[2])
    }

    open fun createFlattenPlanes(i: Int): List<GLModel> {
        val pointers = Native.model_create_flatten_planes(pointer, i)
        val models = ArrayList<GLModel>(pointers.size)
        for (modelPointer in pointers) models.add(GLModel(modelPointer))
        return models
    }

    open fun getExtruder(i: Int): Int = Native.model_get_extruder(pointer, i)

    open fun setExtruder(i: Int, extruder: Int) {
        Native.model_set_extruder(pointer, i, extruder)
    }

    /** One-based filament indices used by parts and painted facets of this object. */
    open fun getUsedFilaments(i: Int): IntArray = Native.model_get_used_filaments(pointer, i)

    open fun setUniformFilament(i: Int, filament: Int) {
        Native.model_set_uniform_filament(pointer, i, filament)
    }

    open fun applyAdaptiveLayerHeight(i: Int, configPath: String, qualityFactor: Float) {
        Native.model_apply_adaptive_layer_height(pointer, i, configPath, qualityFactor)
    }

    open fun clearAdaptiveLayerHeight(i: Int) {
        Native.model_clear_adaptive_layer_height(pointer, i)
    }

    open fun hasAdaptiveLayerHeight(i: Int): Boolean = Native.model_has_adaptive_layer_height(pointer, i)

    open fun embossText(
        i: Int,
        fontPath: String,
        text: String,
        size: Float,
        depth: Float,
        type: Int,
        position: Vec3d,
        normal: Vec3d,
    ) {
        Native.model_emboss_text(
            pointer, i, fontPath, text, size, depth, type,
            position.x, position.y, position.z, normal.x, normal.y, normal.z,
        )
        resetBoundingBox()
    }

    /** True if object i has any committed multi-color painting. */
    open fun hasPaint(i: Int): Boolean = Native.model_has_paint(pointer, i)

    /** Fill a GLModel with object i's committed painting for the given filament; returns triangle count. */
    open fun buildPaintOverlay(i: Int, gl: GLModel, filamentIdx: Int): Int =
        Native.model_build_paint_overlay(pointer, i, gl.pointer, filamentIdx)

    /** Highest filament index (1-based) painted on object i, or 0 if none. */
    open fun paintMaxFilament(i: Int): Int = Native.model_paint_max_filament(pointer, i)

    /** Highest 1-based tool referenced by the model's base assignments or painted facets. */
    open val requiredFilamentCount: Int
        get() {
            var count = 1
            for (i in 0 until objectsCount) {
                count = maxOf(count, getUsedFilaments(i).maxOrNull() ?: 1)
            }
            return count
        }

    open fun autoOrient(i: Int) {
        Native.model_auto_orient(pointer, i)
        resetBoundingBox()
    }

    open fun isBigObject(i: Int): Boolean = Native.model_is_big_object(pointer, i)

    @Throws(Slic3rRuntimeError::class)
    open fun slice(configPath: String, gcodePath: String, listener: SliceListener?): GCodeProcessorResult =
        slice(configPath, gcodePath, listener, 0, 0.0, 0.0, 0.0)

    /** Slice as an OrcaSlicer calibration (calibMode = CalibMode ordinal; 0 = normal print). */
    @Throws(Slic3rRuntimeError::class)
    open fun slice(
        configPath: String,
        gcodePath: String,
        listener: SliceListener?,
        calibMode: Int,
        calibStart: Double,
        calibEnd: Double,
        calibStep: Double,
    ): GCodeProcessorResult {
        restoreAutoBrimForNative(configPath)

        // Declare enough filament slots for object assignments and painted facets so native
        // slicing emits all required multi-material tool changes.
        val numFilaments = requiredFilamentCount
        val palette = Prefs.getFilamentPalette()
        val colors = if (numFilaments > 1) {
            IntArray(numFilaments) { i -> palette[minOf(i, palette.size - 1)] and 0xFFFFFF }
        } else {
            null
        }
        // Native slicing can emit progress before generating G-code. Keep the nullable public API
        // safe by supplying a no-op callback when callers do not need progress updates.
        val nativeListener = listener ?: SliceListener { _, _ -> }
        val resultPointer = Native.model_slice(
            pointer, configPath, gcodePath, nativeListener, numFilaments, colors ?: IntArray(0),
            calibMode, calibStart, calibEnd, calibStep,
        )
        return GCodeProcessorResult(resultPointer)
    }

    @Throws(Slic3rRuntimeError::class)
    open fun export3mf(configPath: String, _3mfPath: String) {
        restoreAutoBrimForNative(configPath)
        Native.model_export_3mf(pointer, configPath, _3mfPath)
    }

    open fun release() {
        if (pointer != 0L) {
            Native.model_release(pointer)
            pointer = 0L
            resetBoundingBox()
        }
    }

    @Suppress("deprecation")
    @Throws(Throwable::class)
    protected open fun finalize() {
        release()
    }

    private fun restoreAutoBrimForNative(configPath: String) {
        if (!SliceBeam.AUTO_BRIM_SELECTED) return
        try {
            val configFile = File(configPath)
            val config = readUtf8File(configFile)
            // Only replace outer_only → auto_brim to avoid mangling other brim_type values.
            if (config.contains("brim_type = outer_only")) {
                writeUtf8File(configFile, config.replace("brim_type = outer_only", "brim_type = auto_brim"))
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private fun readUtf8File(file: File): String {
        val output = ByteArrayOutputStream()
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
            }
        }
        return String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun writeUtf8File(file: File, content: String) {
        FileOutputStream(file, false).use { output -> output.write(content.toByteArray(StandardCharsets.UTF_8)) }
    }

    companion object {
        private fun readModelPointer(path: String, plateId: Int): Long {
            val result = OrcaModelBridge.load(path, plateId)
            if (!result.isSuccess()) {
                throw Slic3rRuntimeError(result.errorMessage, result.cause)
            }
            return result.pointer
        }

        @JvmStatic
        fun merge(vararg models: Model): Model {
            val pointers = LongArray(models.size) { i -> models[i].pointer }
            return Model(Native.models_merge(*pointers))
        }
    }
}
