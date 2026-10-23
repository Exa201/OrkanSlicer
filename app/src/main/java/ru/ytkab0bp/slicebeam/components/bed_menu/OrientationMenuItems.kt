package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import com.google.android.material.snackbar.Snackbar
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import java.util.function.Consumer

/** Kotlin action controller for the live arrange/orient/flatten menu. */
internal class OrientationMenuItems(
    private val fragment: BedFragment,
    private val showPositionMenu: Consumer<View>,
    private val showRotationMenu: Consumer<View>,
    private val onItemsChanged: () -> Unit,
) {
    private var flattenEnabled = false

    fun createItems(): List<BedComposeMenuAction> {
        val renderer = fragment.glView!!.renderer
        val availability = modelSelectionAvailability(renderer.model != null, renderer.selectedObject)

        return listOf(
            BedComposeMenuAction(
                R.string.MenuOrientationArrange,
                R.drawable.grid_layout_outline_28,
                {
                    val glView = fragment.glView ?: return@BedComposeMenuAction
                    glView.arrange { arranged ->
                        if (arranged) {
                            glView.queueEvent {
                                if (renderer.invalidateFlattenMode()) glView.requestRender()
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
                    if (renderer.resetFlattenMode()) {
                        fragment.glView!!.requestRender()
                        resetFlattenCheckedState()
                    }
                    val selected = renderer.selectedObject
                    if (selected != -1) {
                        renderer.model!!.autoOrient(selected)
                        renderer.invalidateGlModel(selected)
                        fragment.glView!!.requestRender()
                        SliceBeam.EVENT_BUS.fireEvent(
                            NeedSnackbarEvent(R.string.MenuOrientationAutoOrientDone, Snackbar.LENGTH_SHORT),
                        )
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
                    renderer.setInFlattenMode(flattenEnabled)
                    fragment.glView!!.requestRender()
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
                    resetFlattenIfNeeded(renderer)
                    showPositionMenu.accept(anchor)
                },
                enabled = availability.hasSelectedObject,
            ),
            BedComposeMenuAction(
                R.string.MenuOrientationRotation,
                R.drawable.menu_orientation_rotation_28,
                { anchor ->
                    resetFlattenIfNeeded(renderer)
                    showRotationMenu.accept(anchor)
                },
                enabled = availability.hasSelectedObject,
            ),
        )
    }

    fun onFlattenModeReset() = resetFlattenCheckedState()

    fun onObjectsChanged() = resetFlattenCheckedState()

    fun onSelectionChanged() = resetFlattenCheckedState()

    private fun resetFlattenIfNeeded(renderer: ru.ytkab0bp.slicebeam.render.GLRenderer) {
        if (renderer.resetFlattenMode()) {
            fragment.glView!!.requestRender()
            resetFlattenCheckedState()
        }
    }

    private fun resetFlattenCheckedState() {
        if (!flattenEnabled) return
        flattenEnabled = false
        onItemsChanged()
    }
}
