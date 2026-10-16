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
) {
    private var text: String = context.getString(R.string.AppName)
    private var size: Float = context.getString(R.string.EmbossDefaultFontSize).toFloatOrNull() ?: 10f
    private var depth: Float = context.getString(R.string.EmbossDefaultDepth).toFloatOrNull() ?: 1f
    private var addMode: Boolean = true
    private var fontFilePath: String? = null
    private val eventBridge = EmbossModeEventBridge(::onSurfaceClicked)

    fun start() {
        SliceBeam.EVENT_BUS.registerListener(eventBridge)
    }

    fun stop() {
        SliceBeam.EVENT_BUS.unregisterListener(eventBridge)
    }

    fun update(text: String, size: String, depth: String, addMode: Boolean) {
        this.text = text
        this.size = size.replace(',', '.').toFloatOrNull() ?: 10f
        this.depth = depth.replace(',', '.').toFloatOrNull() ?: 1f
        this.addMode = addMode
    }

    private fun onSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        val selectedObject = glView.renderer.selectedObject
        if (selectedObject == -1) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.SelectObjectFirst))
            return
        }
        val fontPath = fontFilePath ?: prepareFontFile()?.also { fontFilePath = it }
        if (fontPath == null) {
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossFailed, "Schriftart konnte nicht geladen werden"))
            return
        }
        val safeText = text.trim().ifEmpty { "text" }
        val safeSize = size
        val safeDepth = depth
        val operationType = if (addMode) 0 else 1
        glView.queueEvent {
            try {
                glView.renderer.model!!.embossText(
                    selectedObject,
                    fontPath,
                    safeText,
                    safeSize,
                    safeDepth,
                    operationType,
                    event.position,
                    event.normal,
                )
                glView.renderer.invalidateGlModel(selectedObject)
                glView.requestRender()
                ViewUtils.postOnMainThread { SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.EmbossSuccess)) }
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
