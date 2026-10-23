package ru.ytkab0bp.slicebeam.view

import android.content.Context
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.EmbossSurfaceClickedEvent
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File

/** Keeps emboss event handling and GL mutations outside the Compose presentation. */
internal class EmbossModeController(
    private val context: Context,
    private val glView: GLView,
    private val onModelChanged: () -> Unit = {},
    initialStandaloneMode: Boolean = false,
) {
    private val initialText = context.getString(R.string.AppName)
    private var parameters: EmbossTextInputValidator.Result = EmbossTextInputValidator.validate(
        initialText,
        context.getString(R.string.EmbossDefaultFontSize),
        context.getString(R.string.EmbossDefaultDepth),
    )
    private var addMode: Boolean = true
    private var standaloneMode: Boolean = initialStandaloneMode
    private var followSurface: Boolean = false
    private var fontFilePath: String? = null
    private val eventBridge = EmbossModeEventBridge(::onSurfaceClicked)

    fun start() {
        glView.renderer.isStandaloneTextPlacementMode = standaloneMode
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
    }

    fun stop() {
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
    ) {
        parameters = EmbossTextInputValidator.validate(text, size, depth)
        this.addMode = addMode
        this.standaloneMode = standaloneMode
        this.followSurface = standaloneMode && followSurface
        glView.renderer.isStandaloneTextPlacementMode = standaloneMode
    }

    private fun onSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        val validParameters = parameters
        if (validParameters.value == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossInvalidParameters))
            return
        }
        val selectedObject = glView.renderer.selectedObject
        if ((!standaloneMode && selectedObject == -1) || (event.isBuildPlatePlacement && !standaloneMode)) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.SelectObjectFirst))
            return
        }
        val fontPath = fontFilePath ?: prepareFontFile()?.also { fontFilePath = it }
        if (fontPath == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, "Schriftart konnte nicht geladen werden"))
            return
        }
        val safeText = validParameters.value.text
        val safeSize = validParameters.value.size
        val safeDepth = validParameters.value.depth
        val operationType = if (addMode) 0 else 1
        val createSeparateObject = standaloneMode
        glView.queueEvent {
            try {
                val model = glView.renderer.model ?: return@queueEvent
                val affectedObject = if (createSeparateObject) {
                    model.createTextObject(
                        fontPath, safeText, safeSize, safeDepth, event.position, event.normal,
                        targetObjectIndex = if (followSurface && !event.isBuildPlatePlacement) event.targetObjectIndex else -1,
                    )
                        .also { if (it < 0) throw IllegalStateException("Text model could not be created") }
                } else {
                    model.embossText(
                        selectedObject, fontPath, safeText, safeSize, safeDepth, operationType,
                        event.position, event.normal,
                    )
                    selectedObject
                }
                glView.renderer.invalidateGlModel(affectedObject)
                if (createSeparateObject) glView.renderer.setSelectedObject(affectedObject)
                glView.requestRender()
                ViewUtils.postOnMainThread {
                    onModelChanged()
                    SliceBeam.EVENT_BUS.fireEvent(
                        NeedSnackbarEvent(if (createSeparateObject) R.string.TextModelCreated else R.string.EmbossSuccess),
                    )
                }
            } catch (error: Exception) {
                ViewUtils.postOnMainThread {
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, error.message))
                }
            }
        }
    }

    private fun prepareFontFile(): String? = runCatching {
        val fontFile = File(context.cacheDir, "roboto_medium.ttf")
        if (!fontFile.exists()) {
            context.assets.open("font/roboto_medium.ttf").use { input ->
                fontFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        fontFile.absolutePath
    }.getOrNull()
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
