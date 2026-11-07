package ru.ytkab0bp.slicebeam.view

import android.content.Context
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.EmbossSurfaceClickedEvent
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.io.File

/** Keeps emboss event handling and GL mutations outside the Compose presentation. */
internal class EmbossModeController(
    private val context: Context,
    private val glView: GLView,
    private val onModelChanged: () -> Unit = {},
    initialStandaloneMode: Boolean = false,
) {
    private val initialText = context.getString(R.string.AppName)
    @Volatile private var options = EmbossModeOptions.create(
        parameters = EmbossTextInputValidator.validate(
            initialText,
            context.getString(R.string.EmbossDefaultFontSize),
            context.getString(R.string.EmbossDefaultDepth),
        ),
        addMode = true,
        standaloneMode = initialStandaloneMode,
        followSurface = false,
    )
    @Volatile private var fontFilePath: String? = null
    private val sessionGate = EmbossSessionGate()
    private val eventBridge = EmbossModeEventBridge(::onSurfaceClicked)

    fun start() {
        if (sessionGate.begin() == null) return
        fontFilePath = resolveFontFile(EmbossFontPreset.ROBOTO_MEDIUM)
        glView.renderer.isStandaloneTextPlacementMode = options.standaloneMode
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
    }

    fun stop() {
        if (!sessionGate.end()) return
        glView.renderer.isStandaloneTextPlacementMode = false
        SliceBeam.EVENT_BUS.unregisterListener(eventBridge)
    }

    fun update(
        text: String,
        size: String,
        depth: String,
        addMode: Boolean,
        standaloneMode: Boolean = false,
        followSurface: Boolean = false,
        fontPreset: EmbossFontPreset = EmbossFontPreset.ROBOTO_MEDIUM,
        standaloneFilament: Int = 1,
    ) {
        options = EmbossModeOptions.create(
            parameters = EmbossTextInputValidator.validate(text, size, depth),
            addMode = addMode,
            standaloneMode = standaloneMode,
            followSurface = followSurface,
            standaloneFilament = standaloneFilament,
        )
        fontFilePath = resolveFontFile(fontPreset) ?: fontFilePath
        glView.renderer.isStandaloneTextPlacementMode = standaloneMode
    }

    fun availableFontPresets(): List<EmbossFontPreset> =
        EmbossFontPreset.entries.filter { it.isAvailable() }

    private fun onSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        val session = sessionGate.current() ?: return
        // One immutable snapshot prevents a GL tap from combining old and new UI modes.
        val editOptions = options
        val validParameters = editOptions.parameters
        if (validParameters.value == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossInvalidParameters))
            return
        }
        // The bridge runs on the EventBus worker thread. The clicked object index is
        // captured by GLRenderer and avoids reading mutable renderer state off-GL.
        val selectedObject = event.targetObjectIndex
        if ((!editOptions.standaloneMode && selectedObject == -1) ||
            (event.isBuildPlatePlacement && !editOptions.standaloneMode)
        ) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.SelectObjectFirst))
            return
        }
        val fontPath = fontFilePath
        if (fontPath == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, "Schriftart konnte nicht geladen werden"))
            return
        }
        val safeText = validParameters.value.text
        val safeSize = validParameters.value.size
        val safeDepth = validParameters.value.depth
        val createSeparateObject = editOptions.standaloneMode
        glView.queueEvent {
            // Back or a tool switch invalidates taps already queued for the GL thread.
            if (!sessionGate.isCurrent(session)) return@queueEvent
            try {
                val model = glView.renderer.model ?: return@queueEvent
                val affectedObject = if (createSeparateObject) {
                    val targetObjectIndex = if (editOptions.followSurface && !event.isBuildPlatePlacement) {
                        event.targetObjectIndex
                    } else {
                        -1
                    }
                    model.createTextObject(
                        fontPath, safeText, safeSize, safeDepth, event.position, event.normal,
                        targetObjectIndex = targetObjectIndex,
                    )
                        .also {
                            if (it < 0) throw IllegalStateException("Text model could not be created")
                            // Standalone text is a separate printable object and can have
                            // its own slicer filament. Surface-following text is a volume
                            // of the target, so retain that object's existing material.
                            if (targetObjectIndex < 0) {
                                model.setUniformFilament(
                                    it,
                                    editOptions.standaloneFilament.coerceIn(1, Prefs.MAX_FILAMENT_COLORS),
                                )
                            }
                        }
                } else {
                    model.embossText(
                        selectedObject, fontPath, safeText, safeSize, safeDepth,
                        if (editOptions.addMode) 0 else 1,
                        event.position, event.normal,
                    )
                    selectedObject
                }
                glView.renderer.invalidateGlModel(affectedObject)
                if (createSeparateObject) glView.renderer.setSelectedObject(affectedObject)
                // Queue the model-history commit from the GL thread before the new object
                // becomes observable to UI actions such as Undo. Posting this callback to
                // the main thread first allowed a fast Undo to run before updateModel had
                // queued the pre-edit snapshot.
                onModelChanged()
                glView.requestRender()
                ViewUtils.postOnMainThread {
                    SliceBeam.EVENT_BUS.fireEvent(
                        NeedSnackbarEvent(if (createSeparateObject) R.string.TextModelCreated else R.string.EmbossSuccess),
                    )
                }
            } catch (error: Exception) {
                ViewUtils.postOnMainThread {
                    val errorMessage = error.message
                    if (errorMessage?.contains("complete text must fit on the visible model surface") == true) {
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.TextModelSurfaceFitError))
                    } else {
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, errorMessage))
                    }
                }
            }
        }
    }

    private fun resolveFontFile(preset: EmbossFontPreset): String? = runCatching {
        preset.systemFontPath?.let { path ->
            return@runCatching File(path).takeIf { it.isFile && it.canRead() }?.absolutePath
        }
        val fontFile = File(context.cacheDir, "roboto_medium.ttf")
        if (!fontFile.exists()) {
            context.assets.open("font/roboto_medium.ttf").use { input ->
                fontFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        fontFile.absolutePath
    }.getOrNull()
}

/** Fonts available to native text extrusion. System fonts are offered only when readable. */
internal enum class EmbossFontPreset(
    val labelRes: Int,
    internal val systemFontPath: String? = null,
) {
    ROBOTO_MEDIUM(R.string.EmbossFontRoboto),
    SERIF(R.string.EmbossFontSerif, "/system/fonts/NotoSerif-Regular.ttf"),
    MONOSPACE(R.string.EmbossFontMonospace, "/system/fonts/DroidSansMono.ttf");

    fun isAvailable(): Boolean = systemFontPath?.let { path ->
        File(path).let { it.isFile && it.canRead() }
    } ?: true
}

/** Invalidates callbacks already queued on GL when the text tool leaves the workspace. */
internal class EmbossSessionGate {
    @Volatile private var activeSession: Any? = null

    @Synchronized
    fun begin(): Any? {
        if (activeSession != null) return null
        return Any().also { activeSession = it }
    }

    @Synchronized
    fun end(): Boolean {
        if (activeSession == null) return false
        activeSession = null
        return true
    }

    fun current(): Any? = activeSession

    fun isCurrent(session: Any): Boolean = activeSession === session
}

/** Immutable options published as one volatile reference to the GL/EventBus callback. */
internal class EmbossModeOptions private constructor(
    val parameters: EmbossTextInputValidator.Result,
    val addMode: Boolean,
    val standaloneMode: Boolean,
    val followSurface: Boolean,
    val standaloneFilament: Int,
) {
    companion object {
        fun create(
            parameters: EmbossTextInputValidator.Result,
            addMode: Boolean,
            standaloneMode: Boolean,
            followSurface: Boolean,
            standaloneFilament: Int = 1,
        ) = EmbossModeOptions(
            parameters,
            addMode,
            standaloneMode,
            standaloneMode && followSurface,
            standaloneFilament.coerceIn(1, Prefs.MAX_FILAMENT_COLORS),
        )
    }
}

/** Validates model-generation input before it reaches native geometry code. */
internal object EmbossTextInputValidator {
    data class Parameters(val text: String, val size: Float, val depth: Float)
    data class Result(val value: Parameters?, val error: Error?)
    enum class Error { EMPTY_TEXT, TEXT_TOO_LONG, INVALID_SIZE, INVALID_DEPTH }

    fun validate(text: String, size: String, depth: String): Result {
        val normalizedText = text.trim()
        val error = when {
            normalizedText.isEmpty() -> Error.EMPTY_TEXT
            normalizedText.codePointCount(0, normalizedText.length) > MAX_TEXT_CODE_POINTS -> Error.TEXT_TOO_LONG
            parsePositive(size) == null -> Error.INVALID_SIZE
            parsePositive(depth) == null -> Error.INVALID_DEPTH
            else -> null
        }
        if (error != null) return Result(null, error)
        return Result(
            Parameters(normalizedText, requireNotNull(parsePositive(size)), requireNotNull(parsePositive(depth))),
            null,
        )
    }

    private fun parsePositive(value: String): Float? = value.replace(',', '.').toFloatOrNull()
        ?.takeIf { it.isFinite() && it > 0f }

    private const val MAX_TEXT_CODE_POINTS = 256
}
