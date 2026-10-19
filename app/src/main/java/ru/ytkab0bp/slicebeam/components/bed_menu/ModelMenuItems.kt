package ru.ytkab0bp.slicebeam.components.bed_menu

import android.widget.Toast
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Model selection, placement and object operations; geometry editing stays in TransformMenuItems. */
internal class ModelMenuItems(
    private val fragment: BedFragment,
    private val onItemsChanged: () -> Unit,
) {
    private var flattenEnabled = false

    fun createItems(): List<BedComposeMenuAction> {
        val renderer = fragment.glView!!.renderer
        val availability = modelSelectionAvailability(renderer.model != null, renderer.selectedObject)
        val objectActions = FileMenuObjectActions(fragment)
        return listOf(
            BedComposeMenuAction(R.string.WorkspaceObjects, R.drawable.square_stack_up_outline_28, {
                objectActions.showObjectsDialog(it)
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuDuplicate, R.drawable.folder_simple_plus_outline_28, {
                objectActions.showDuplicateDialog(it)
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuMoveToPlate, R.drawable.square_stack_up_outline_28, {
                objectActions.showMoveToPlateDialog(it)
            }, enabled = availability.hasSelectedObject && fragment.plateCount > 1),
            BedComposeMenuAction(R.string.ModelMenuCenterOnBed, R.drawable.view_in_ar_24, { anchor ->
                fragment.glView!!.centerSelectedOnBed {
                    fragment.updateModel()
                    Toast.makeText(anchor.context, R.string.ModelContextCentered, Toast.LENGTH_SHORT).show()
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuFillBed, R.drawable.picture_stack_outline_28, { anchor ->
                fragment.glView!!.fillBedWithSelectedModel { addedCopies ->
                    fragment.updateModel()
                    Toast.makeText(
                        anchor.context,
                        if (addedCopies > 0) anchor.resources.getQuantityString(
                            R.plurals.ModelContextFillBedFinished, addedCopies, addedCopies,
                        ) else anchor.context.getString(R.string.ModelContextFillBedNoRoom),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuArrange, R.drawable.grid_layout_outline_28, {
                fragment.glView!!.arrange()
                fragment.glView!!.queueEvent {
                    if (renderer.invalidateFlattenMode()) fragment.glView!!.requestRender()
                }
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientationArrangeFinished))
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuOrientAll, R.drawable.menu_orientation_auto_28, {
                fragment.glView!!.queueEvent {
                    val model = renderer.model ?: return@queueEvent
                    if (renderer.invalidateFlattenMode()) fragment.glView!!.requestRender()
                    for (index in 0 until model.objectsCount) {
                        model.autoOrient(index)
                        renderer.invalidateGlModel(index)
                    }
                    fragment.glView!!.requestRender()
                    ViewUtils.postOnMainThread {
                        fragment.updateModel()
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientAllObjectsDone))
                    }
                }
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuAutoOrientSelected, R.drawable.menu_orientation_auto_28, {
                fragment.glView!!.queueEvent {
                    val model = renderer.model ?: return@queueEvent
                    val index = renderer.selectedObject
                    if (index >= 0) {
                        model.autoOrient(index)
                        renderer.invalidateGlModel(index)
                        fragment.glView!!.requestRender()
                        ViewUtils.postOnMainThread {
                            fragment.updateModel()
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientationAutoOrientDone))
                        }
                    }
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuFlatten, R.drawable.menu_orientation_flatten_28, {
                flattenEnabled = !flattenEnabled
                val flatten = flattenEnabled
                fragment.glView!!.queueEvent {
                    renderer.setInFlattenMode(flatten)
                    fragment.glView!!.requestRender()
                }
                onItemsChanged()
            }, selected = flattenEnabled, toggleable = true, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuDeleteSelected, R.drawable.delete_outline_android_28, { anchor ->
                fragment.showWorkspaceMessageDialog(
                    anchor.context.getString(R.string.ModelMenuDeleteSelected),
                    anchor.context.getString(R.string.ModelDeleteConfirm),
                    anchor.context.getString(R.string.ModelMenuDeleteSelected),
                    anchor.context.getString(android.R.string.cancel),
                ) { fragment.glView!!.deleteSelectedModel(fragment::updateModel) }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuSelectAll, R.drawable.square_stack_up_outline_28, {
                fragment.glView!!.selectAllModels { fragment.updateModel() }
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuDeleteAll, R.drawable.delete_outline_android_28, { anchor ->
                fragment.showWorkspaceMessageDialog(
                    anchor.context.getString(R.string.ModelMenuDeleteAll),
                    anchor.context.getString(R.string.ModelDeleteAllConfirm),
                    anchor.context.getString(R.string.ModelMenuDeleteAll),
                    anchor.context.getString(android.R.string.cancel),
                ) { fragment.glView!!.deleteAllModels { fragment.updateModel() } }
            }, enabled = availability.canArrange),
        )
    }

    fun resetFlattenMode() {
        flattenEnabled = false
    }
}
