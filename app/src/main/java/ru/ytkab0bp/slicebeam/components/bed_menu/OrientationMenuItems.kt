package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import com.google.android.material.snackbar.Snackbar
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.function.Consumer

/** Kotlin action controller for the live arrange/orient/flatten menu. */
internal class OrientationMenuItems(
    private val fragment: BedFragment,
    private val showPositionMenu: Consumer<View>,
    private val showRotationMenu: Consumer<View>,
    private val onItemsChanged: () -> Unit,
) {
    private var flattenEnabled = false
    private val selectionObserver = ModelMenuSelectionObserver(fragment, onItemsChanged)

    fun createItems(): List<BedComposeMenuAction> {
        selectionObserver.readIfNeeded()
        val selection = selectionObserver.state.value
        val availability = modelSelectionAvailability(selection.modelLoaded, selection.selectedObject)
        val glView = fragment.glView

        return listOf(
            BedComposeMenuAction(
                R.string.MenuOrientationArrange,
                R.drawable.grid_layout_outline_28,
                {
                    val activeGlView = glView ?: return@BedComposeMenuAction
                    activeGlView.arrange { arranged ->
                        if (arranged) {
                            activeGlView.queueEvent {
                                if (activeGlView.renderer.invalidateFlattenMode()) activeGlView.requestRender()
                            }
                            fragment.updateModel()
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientationArrangeFinished))
                        } else {
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelContextFillBedNoRoom))
                        }
                    }
                },
                enabled = availability.canArrange,
            ),
            BedComposeMenuAction(
                R.string.MenuOrientationAutoOrient,
                R.drawable.menu_orientation_auto_28,
                {
                    val activeGlView = glView ?: return@BedComposeMenuAction
                    activeGlView.queueEvent {
                        val renderer = activeGlView.renderer
                        val selected = renderer.selectedObject
                        val model = renderer.model
                        if (selected !in 0 until (model?.objectsCount ?: 0)) return@queueEvent
                        if (renderer.resetFlattenMode()) activeGlView.requestRender()
                        model!!.autoOrient(selected)
                        renderer.invalidateGlModel(selected)
                        activeGlView.requestRender()
                        ViewUtils.postOnMainThread {
                            resetFlattenCheckedState()
                            SliceBeam.EVENT_BUS.fireEvent(
                                NeedSnackbarEvent(R.string.MenuOrientationAutoOrientDone, Snackbar.LENGTH_SHORT),
                            )
                            fragment.updateModel()
                        }
                    }
                },
                sectionBreakBefore = true,
                enabled = availability.hasSelectedObject,
            ),
            BedComposeMenuAction(
                R.string.MenuOrientationFlatten,
                R.drawable.menu_orientation_flatten_28,
                {
                    flattenEnabled = !flattenEnabled
                    val requestedFlatten = flattenEnabled
                    val activeGlView = glView ?: return@BedComposeMenuAction
                    activeGlView.queueEvent {
                        val renderer = activeGlView.renderer
                        val model = renderer.model
                        if (requestedFlatten && renderer.selectedObject !in 0 until (model?.objectsCount ?: 0)) {
                            ViewUtils.postOnMainThread {
                                flattenEnabled = false
                                onItemsChanged()
                            }
                            return@queueEvent
                        }
                        renderer.setInFlattenMode(requestedFlatten)
                        activeGlView.requestRender()
                    }
                    onItemsChanged()
                },
                selected = flattenEnabled,
                toggleable = true,
                enabled = availability.hasSelectedObject,
            ),
            BedComposeMenuAction(
                R.string.MenuOrientationPosition,
                R.drawable.menu_orientation_position_28,
                { anchor ->
                    resetFlattenIfNeeded()
                    showPositionMenu.accept(anchor)
                },
                enabled = availability.hasSelectedObject,
            ),
            BedComposeMenuAction(
                R.string.MenuOrientationRotation,
                R.drawable.menu_orientation_rotation_28,
                { anchor ->
                    resetFlattenIfNeeded()
                    showRotationMenu.accept(anchor)
                },
                enabled = availability.hasSelectedObject,
            ),
        )
    }

    fun onFlattenModeReset() = resetFlattenCheckedState()

    fun onObjectsChanged() {
        selectionObserver.invalidate()
        resetFlattenCheckedState()
    }

    fun onSelectionChanged() {
        selectionObserver.invalidate()
        resetFlattenCheckedState()
    }

    private fun resetFlattenIfNeeded() {
        val glView = fragment.glView ?: return
        glView.queueEvent {
            if (glView.renderer.resetFlattenMode()) {
                glView.requestRender()
                ViewUtils.postOnMainThread { resetFlattenCheckedState() }
            }
        }
    }

    private fun resetFlattenCheckedState() {
        if (!flattenEnabled) return
        flattenEnabled = false
        onItemsChanged()
    }
}
