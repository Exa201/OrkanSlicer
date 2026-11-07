package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils

internal data class ModelSelectionAvailability(
    val canArrange: Boolean,
    val hasSelectedObject: Boolean,
)

/** Shares the legacy menu's exact model/selection enablement rule across Compose menus. */
internal fun modelSelectionAvailability(modelLoaded: Boolean, selectedObjectIndex: Int): ModelSelectionAvailability =
    ModelSelectionAvailability(
        canArrange = modelLoaded,
        hasSelectedObject = modelLoaded && selectedObjectIndex != -1,
    )

/** Immutable menu-facing selection summary captured on the renderer's GL thread. */
internal data class ModelMenuSelectionState(
    val modelLoaded: Boolean = false,
    val selectedObject: Int = -1,
    val selectedObjectCount: Int = 0,
    val objectCount: Int = 0,
)

/** Keeps Compose menu enablement off the renderer's unsynchronized GL-owned fields. */
internal class ModelMenuSelectionObserver(
    private val fragment: BedFragment,
    private val onChanged: () -> Unit,
) {
    val state: MutableState<ModelMenuSelectionState> = mutableStateOf(ModelMenuSelectionState())
    private var revision = 0L
    private var observedRevision = -1L
    private var readInFlight = false

    fun invalidate() {
        revision++
        state.value = ModelMenuSelectionState()
    }

    fun readIfNeeded() {
        if (readInFlight || observedRevision == revision) return
        val glView = fragment.glView ?: return
        readInFlight = true
        val requestedRevision = revision
        glView.queueEvent {
            val renderer = glView.renderer
            val model = renderer.model
            val snapshot = ModelMenuSelectionState(
                modelLoaded = model != null,
                selectedObject = renderer.selectedObject,
                selectedObjectCount = renderer.selectedObjectsCount,
                objectCount = model?.objectsCount ?: 0,
            )
            ViewUtils.postOnMainThread {
                readInFlight = false
                if (requestedRevision != revision) {
                    readIfNeeded()
                    return@postOnMainThread
                }
                observedRevision = requestedRevision
                if (state.value != snapshot) {
                    state.value = snapshot
                    onChanged()
                }
            }
        }
    }
}
