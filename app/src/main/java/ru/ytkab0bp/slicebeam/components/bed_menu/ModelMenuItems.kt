package ru.ytkab0bp.slicebeam.components.bed_menu

import android.widget.Toast
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.slic3r.ObjectBounds3D
import ru.ytkab0bp.slicebeam.slic3r.ObjectPlacement
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation

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
        val model = renderer.model
        val selectedCount = renderer.selectedObjectsCount
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
            BedComposeMenuAction(R.string.ModelMenuPlaceOnObject, R.drawable.view_in_ar_24, { anchor ->
                showPlaceOnObjectDialog(anchor)
            }, enabled = availability.hasSelectedObject && (model?.objectsCount ?: 0) > selectedCount),
            BedComposeMenuAction(R.string.ModelMenuCombineSelected, R.drawable.square_stack_up_outline_28, { anchor ->
                val glView = fragment.glView ?: return@BedComposeMenuAction
                fragment.showWorkspaceMessageDialog(
                    anchor.context.getString(R.string.ModelMenuCombineSelected),
                    anchor.context.getString(R.string.ModelMenuCombineWarning),
                    anchor.context.getString(R.string.ModelMenuCombineSelected),
                    anchor.context.getString(android.R.string.cancel),
                ) {
                    glView.combineSelectedObjectsAsParts { result ->
                        if (result >= 0) {
                            fragment.updateModel()
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuCombineDone))
                        } else {
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuCombineUnsupported))
                        }
                    }
                }
            }, enabled = selectedCount >= 2),
            BedComposeMenuAction(R.string.ModelMenuBoolean, R.drawable.square_stack_up_outline_28, { anchor ->
                showBooleanDialog(anchor)
            }, enabled = selectedCount >= 2),
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

    private fun showPlaceOnObjectDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        val model = glView.renderer.model ?: return
        val movingIndices = glView.renderer.selectedObjectIndices()
            .filter { it in 0 until model.objectsCount }
        if (movingIndices.isEmpty()) return
        val targetIndices = (0 until model.objectsCount).filter { it !in movingIndices }
        val choices = targetIndices.map { index ->
            model.getObjectName(index) ?: anchor.context.getString(R.string.MenuFileObjectNumber, index + 1)
        }
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = anchor.context.getString(R.string.ModelMenuPlaceOnObject),
                cancelLabel = anchor.context.getString(android.R.string.cancel),
                choices = choices,
                onDismiss = dismiss,
                onSelect = { choice ->
                    dismiss()
                    val target = targetIndices.getOrNull(choice) ?: return@ModelChoiceDialog
                    glView.queueEvent {
                        val current = glView.renderer.model ?: return@queueEvent
                        if (target !in 0 until current.objectsCount || movingIndices.any { it !in 0 until current.objectsCount }) return@queueEvent
                        fun bounds(index: Int): ObjectBounds3D {
                            val min = Vec3d()
                            val max = Vec3d()
                            current.getBoundingBoxExact(index, min, max)
                            return ObjectBounds3D(min.x, min.y, min.z, max.x, max.y, max.z)
                        }
                        val movingBounds = ObjectPlacement.enclosing(movingIndices.map(::bounds)) ?: return@queueEvent
                        val delta = ObjectPlacement.onTop(movingBounds, bounds(target), clearanceMm = 0.0)
                        if (delta == null) return@queueEvent
                        movingIndices.forEach { index ->
                            current.translate(index, delta.x, delta.y, delta.z)
                            glView.renderer.invalidateGlModel(index)
                        }
                        glView.requestRender()
                        ViewUtils.postOnMainThread { fragment.updateModel() }
                    }
                },
            )
        }
    }

    private fun showBooleanDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        val renderer = glView.renderer
        val model = renderer.model ?: return
        val selected = renderer.selectedObjectIndices()
        if (selected.size < 2) return
        val targetIndex = renderer.selectedObject.takeIf { it in selected } ?: selected[0]
        val orderedIndices = intArrayOf(targetIndex) + selected.filter { it != targetIndex }
        val toolIndices = orderedIndices.drop(1)
        val targetName = model.getObjectName(targetIndex)
            ?: anchor.context.getString(R.string.MenuFileObjectNumber, targetIndex + 1)
        val toolNames = toolIndices.map { index -> model.getObjectName(index)
            ?: anchor.context.getString(R.string.MenuFileObjectNumber, index + 1) }

        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelBooleanDialog(
                onDismiss = dismiss,
                onChoose = { operation ->
                    dismiss()
                    glView.queueEvent {
                        val currentRenderer = glView.renderer
                        val currentModel = currentRenderer.model ?: return@queueEvent
                        val result = currentModel.booleanObjects(orderedIndices, operation)
                        if (result >= 0) {
                            currentRenderer.resetGlModels()
                            currentRenderer.setSelectedObject(result)
                            glView.requestRender()
                            ViewUtils.postOnMainThread {
                                fragment.updateModel()
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelBooleanDone))
                            }
                        } else {
                            ViewUtils.postOnMainThread {
                                SliceBeam.EVENT_BUS.fireEvent(
                                    NeedSnackbarEvent(R.string.ModelBooleanFailed, targetName, toolNames.joinToString()),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
