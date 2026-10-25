package ru.ytkab0bp.slicebeam.components.bed_menu

import android.widget.Toast
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation
import ru.ytkab0bp.slicebeam.render.ModelAlignmentAxis
import ru.ytkab0bp.slicebeam.render.ModelSelectionSnapshot
import ru.ytkab0bp.slicebeam.render.MODEL_EDIT_STALE_SELECTION
import ru.ytkab0bp.slicebeam.slic3r.ObjectDistributionAxis

/** Model selection, placement and object operations; geometry editing stays in TransformMenuItems. */
internal class ModelMenuItems(
    private val fragment: BedFragment,
    private val onItemsChanged: () -> Unit,
) {
    private var flattenEnabled = false
    private val selectionObserver = ModelMenuSelectionObserver(fragment, onItemsChanged)

    /** Selection and model metadata are owned by GLRenderer and must be sampled on its thread. */
    fun invalidateSelectionState() {
        selectionObserver.invalidate()
    }

    fun createItems(): List<BedComposeMenuAction> {
        selectionObserver.readIfNeeded()
        val currentSelection = selectionObserver.state.value
        val availability = modelSelectionAvailability(currentSelection.modelLoaded, currentSelection.selectedObject)
        val objectActions = FileMenuObjectActions(fragment)
        val selectedCount = currentSelection.selectedObjectCount
        val objectCount = currentSelection.objectCount
        val glView = fragment.glView
        val renderer = glView?.renderer
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
                val activeGlView = fragment.glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(activeGlView) { snapshot ->
                    activeGlView.centerSelectedOnBed(snapshot) { result ->
                        when (result) {
                            1 -> {
                                fragment.updateModel()
                                Toast.makeText(anchor.context, R.string.ModelContextCentered, Toast.LENGTH_SHORT).show()
                            }
                            MODEL_EDIT_STALE_SELECTION ->
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                        }
                    }
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuPlaceGroupOnBed, R.drawable.view_in_ar_24, { anchor ->
                val activeGlView = fragment.glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(activeGlView) { snapshot ->
                    activeGlView.placeSelectedObjectsOnBed(snapshot) { result ->
                        when (result) {
                            1 -> {
                                fragment.updateModel()
                                Toast.makeText(anchor.context, R.string.ModelMenuPlaceGroupOnBedDone, Toast.LENGTH_SHORT).show()
                            }
                            MODEL_EDIT_STALE_SELECTION ->
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                            else -> Toast.makeText(
                                anchor.context,
                                R.string.ModelContextFillBedNoRoom,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuPlaceOnObject, R.drawable.view_in_ar_24, { anchor ->
                showPlaceOnObjectDialog(anchor)
            }, enabled = availability.hasSelectedObject && objectCount > selectedCount),
            BedComposeMenuAction(R.string.ModelMenuPlaceOnSurface, R.drawable.view_in_ar_24, { anchor ->
                showPlaceOnSurfaceDialog(anchor)
            }, enabled = availability.hasSelectedObject && objectCount > selectedCount),
            BedComposeMenuAction(R.string.ModelMenuAlignSelected, R.drawable.grid_layout_outline_28, { anchor ->
                showAlignmentDialog(anchor)
            }, enabled = selectedCount >= 2),
            BedComposeMenuAction(R.string.ModelMenuDistributeSelected, R.drawable.grid_layout_outline_28, { anchor ->
                showDistributionDialog(anchor)
            }, enabled = selectedCount >= 3),
            BedComposeMenuAction(R.string.ModelMenuCombineSelected, R.drawable.square_stack_up_outline_28, { anchor ->
                val activeGlView = glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(activeGlView) { snapshot ->
                    if (snapshot.selectedIndices.size < 2) return@captureSelectionSnapshot
                    fragment.showWorkspaceMessageDialog(
                        anchor.context.getString(R.string.ModelMenuCombineSelected),
                        anchor.context.getString(R.string.ModelMenuCombineWarning),
                        anchor.context.getString(R.string.ModelMenuCombineSelected),
                        anchor.context.getString(android.R.string.cancel),
                    ) {
                        activeGlView.combineSelectedObjectsAsParts(snapshot) { result ->
                            when {
                                result >= 0 -> {
                                    fragment.updateModel()
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuCombineDone))
                                }
                                result == MODEL_EDIT_STALE_SELECTION ->
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                                else -> SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuCombineUnsupported))
                            }
                        }
                    }
                }
            }, enabled = selectedCount >= 2),
            BedComposeMenuAction(R.string.ModelMenuBoolean, R.drawable.square_stack_up_outline_28, { anchor ->
                showBooleanDialog(anchor)
            }, enabled = selectedCount >= 2),
            BedComposeMenuAction(R.string.ModelMenuFillBed, R.drawable.picture_stack_outline_28, { anchor ->
                fragment.glView!!.fillBedWithSelectedModel { addedCopies ->
                    if (addedCopies > 0) fragment.updateModel()
                    Toast.makeText(
                        anchor.context,
                        if (addedCopies > 0) anchor.resources.getQuantityString(
                            R.plurals.ModelContextFillBedFinished, addedCopies, addedCopies,
                        ) else anchor.context.getString(R.string.ModelContextFillBedNoRoom),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuArrange, R.drawable.grid_layout_outline_28, { anchor ->
                val activeGlView = glView ?: return@BedComposeMenuAction
                activeGlView.arrange { arranged ->
                    if (!arranged) {
                        Toast.makeText(anchor.context, R.string.ModelContextFillBedNoRoom, Toast.LENGTH_SHORT).show()
                        return@arrange
                    }
                    activeGlView.queueEvent {
                        if (renderer?.invalidateFlattenMode() == true) activeGlView.requestRender()
                    }
                    fragment.updateModel()
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientationArrangeFinished))
                }
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuOrientAll, R.drawable.menu_orientation_auto_28, {
                val activeGlView = fragment.glView ?: return@BedComposeMenuAction
                activeGlView.autoOrientAllObjects { orientedCount ->
                    if (orientedCount > 0) {
                        fragment.updateModel()
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuOrientAllObjectsDone))
                    }
                }
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.TextModelCreateSeparate, R.drawable.edit_outline_28, {
                fragment.enterEmbossMode(standaloneText = true)
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuAutoOrientSelected, R.drawable.menu_orientation_auto_28, {
                val activeGlView = fragment.glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(activeGlView) { snapshot ->
                    activeGlView.autoOrientSelectedObjects(snapshot) { orientedCount ->
                        when {
                            orientedCount > 0 -> {
                                fragment.updateModel()
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(
                                    if (orientedCount == 1) R.string.MenuOrientationAutoOrientDone
                                    else R.string.ModelMenuAutoOrientGroupDone,
                                ))
                            }
                            orientedCount == MODEL_EDIT_STALE_SELECTION ->
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                        }
                    }
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuFlatten, R.drawable.menu_orientation_flatten_28, {
                flattenEnabled = !flattenEnabled
                val flatten = flattenEnabled
                val activeGlView = fragment.glView ?: return@BedComposeMenuAction
                activeGlView.queueEvent {
                    val activeRenderer = activeGlView.renderer
                    val model = activeRenderer.model
                    if (flatten && (model == null || activeRenderer.selectedObject !in 0 until model.objectsCount)) {
                        ViewUtils.postOnMainThread {
                            flattenEnabled = false
                            onItemsChanged()
                        }
                        return@queueEvent
                    }
                    activeRenderer.setInFlattenMode(flatten)
                    activeGlView.requestRender()
                }
                onItemsChanged()
            }, selected = flattenEnabled, toggleable = true, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuDeleteSelected, R.drawable.delete_outline_android_28, { anchor ->
                val glView = fragment.glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(glView) { snapshot ->
                    fragment.showWorkspaceMessageDialog(
                        anchor.context.getString(R.string.ModelMenuDeleteSelected),
                        anchor.context.getString(R.string.ModelDeleteConfirm),
                        anchor.context.getString(R.string.ModelMenuDeleteSelected),
                        anchor.context.getString(android.R.string.cancel),
                    ) {
                        glView.deleteSelectedModelsIfCurrent(snapshot) { removed ->
                            if (removed == MODEL_EDIT_STALE_SELECTION) {
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                            } else if (removed > 0) {
                                fragment.updateModel()
                            }
                        }
                    }
                }
            }, enabled = availability.hasSelectedObject),
            BedComposeMenuAction(R.string.ModelMenuSelectAll, R.drawable.square_stack_up_outline_28, {
                // Selection is renderer UI state, not a model edit. Recording it through
                // updateModel() would add an identical model snapshot and consume the next Undo.
                fragment.glView!!.selectAllModels {}
            }, enabled = availability.canArrange),
            BedComposeMenuAction(R.string.ModelMenuDeleteAll, R.drawable.delete_outline_android_28, { anchor ->
                val activeGlView = glView ?: return@BedComposeMenuAction
                captureSelectionSnapshot(activeGlView, allowEmptySelection = true) { snapshot ->
                    fragment.showWorkspaceMessageDialog(
                        anchor.context.getString(R.string.ModelMenuDeleteAll),
                        anchor.context.getString(R.string.ModelDeleteAllConfirm),
                        anchor.context.getString(R.string.ModelMenuDeleteAll),
                        anchor.context.getString(android.R.string.cancel),
                    ) {
                        activeGlView.deleteAllModelsIfCurrent(snapshot) { removed ->
                            if (removed == MODEL_EDIT_STALE_SELECTION) {
                                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                            } else if (removed > 0) {
                                fragment.updateModel()
                            }
                        }
                    }
                }
            }, enabled = availability.canArrange),
        )
    }

    fun resetFlattenMode() {
        flattenEnabled = false
    }

    private fun showPlaceOnObjectDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        captureTargetChoices { snapshot, targetIndices, names ->
            val choices = names.mapIndexed { index, name ->
                name ?: anchor.context.getString(R.string.MenuFileObjectNumber, targetIndices[index] + 1)
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
                        glView.placeSelectedObjectsOnObject(target, snapshot) { result ->
                            when {
                                result >= 0 -> {
                                    fragment.updateModel()
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuPlaceOnObjectDone))
                                }
                                result == MODEL_EDIT_STALE_SELECTION ->
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                                else -> SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuCombineUnsupported))
                            }
                        }
                    },
                )
            }
        }
    }

    private fun showAlignmentDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        val labels = listOf(
            anchor.context.getString(R.string.ModelMenuAlignCenterX),
            anchor.context.getString(R.string.ModelMenuAlignCenterY),
            anchor.context.getString(R.string.ModelMenuAlignBaseZ),
        )
        captureSelectionSnapshot(glView) { snapshot ->
            fragment.showWorkspaceComposeDialog { dismiss ->
                ModelChoiceDialog(
                    title = anchor.context.getString(R.string.ModelMenuAlignSelected),
                    cancelLabel = anchor.context.getString(android.R.string.cancel),
                    choices = labels,
                    onDismiss = dismiss,
                    onSelect = { choice ->
                        dismiss()
                        val axis = when (choice) {
                            0 -> ModelAlignmentAxis.CENTER_X
                            1 -> ModelAlignmentAxis.CENTER_Y
                            2 -> ModelAlignmentAxis.BASE_Z
                            else -> return@ModelChoiceDialog
                        }
                        glView.alignSelectedObjects(axis, snapshot) { result ->
                            when (result) {
                                1 -> {
                                    fragment.updateModel()
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuAlignDone))
                                }
                                MODEL_EDIT_STALE_SELECTION ->
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                            }
                        }
                    },
                )
            }
        }
    }

    private fun showDistributionDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        val labels = listOf(
            anchor.context.getString(R.string.ModelMenuDistributeX),
            anchor.context.getString(R.string.ModelMenuDistributeY),
            anchor.context.getString(R.string.ModelMenuDistributeZ),
        )
        captureSelectionSnapshot(glView) { snapshot ->
            fragment.showWorkspaceComposeDialog { dismiss ->
                ModelChoiceDialog(
                    title = anchor.context.getString(R.string.ModelMenuDistributeSelected),
                    hint = anchor.context.getString(R.string.ModelMenuDistributeHint),
                    cancelLabel = anchor.context.getString(android.R.string.cancel),
                    choices = labels,
                    onDismiss = dismiss,
                    onSelect = { choice ->
                        dismiss()
                        val axis = when (choice) {
                            0 -> ObjectDistributionAxis.X
                            1 -> ObjectDistributionAxis.Y
                            2 -> ObjectDistributionAxis.Z
                            else -> return@ModelChoiceDialog
                        }
                        glView.distributeSelectedObjects(axis, snapshot) { result ->
                            when (result) {
                                1 -> {
                                    fragment.updateModel()
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuDistributeDone))
                                }
                                MODEL_EDIT_STALE_SELECTION ->
                                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelSelectionChanged))
                                else -> SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelMenuDistributeNoSpace))
                            }
                        }
                    },
                )
            }
        }
    }

    private fun showPlaceOnSurfaceDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        captureTargetChoices { snapshot, targetIndices, names ->
            val choices = names.mapIndexed { index, name ->
                name ?: anchor.context.getString(R.string.MenuFileObjectNumber, targetIndices[index] + 1)
            }
            fragment.showWorkspaceComposeDialog { dismiss ->
                ModelChoiceDialog(
                    title = anchor.context.getString(R.string.ModelMenuPlaceOnSurface),
                    cancelLabel = anchor.context.getString(android.R.string.cancel),
                    choices = choices,
                    onDismiss = dismiss,
                    onSelect = { choice ->
                        dismiss()
                        val target = targetIndices.getOrNull(choice) ?: return@ModelChoiceDialog
                        glView.beginSurfacePlacement(target, snapshot) { result ->
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(
                                when (result) {
                                    1 -> R.string.ModelSurfacePlacementInstruction
                                    MODEL_EDIT_STALE_SELECTION -> R.string.ModelSelectionChanged
                                    else -> R.string.ModelSurfacePlacementTargetUnavailable
                                },
                            ))
                        }
                    },
                )
            }
        }
    }

    private fun captureSelectionSnapshot(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        allowEmptySelection: Boolean = false,
        onCaptured: (ModelSelectionSnapshot) -> Unit,
    ) {
        glView.queueEvent {
            val snapshot = glView.renderer.captureSelectionSnapshot(allowEmptySelection) ?: return@queueEvent
            ViewUtils.postOnMainThread {
                if (fragment.canShowWorkspaceComposeDialog()) onCaptured(snapshot)
            }
        }
    }

    private fun captureTargetChoices(
        onCaptured: (ModelSelectionSnapshot, List<Int>, List<String?>) -> Unit,
    ) {
        val glView = fragment.glView ?: return
        glView.queueEvent {
            val renderer = glView.renderer
            val snapshot = renderer.captureSelectionSnapshot() ?: return@queueEvent
            val targetIndices = (0 until snapshot.objectCount).filter { it !in snapshot.selectedIndices }
            val names = targetIndices.map { snapshot.model.getObjectName(it) }
            ViewUtils.postOnMainThread {
                if (fragment.canShowWorkspaceComposeDialog()) onCaptured(snapshot, targetIndices, names)
            }
        }
    }

    private fun showBooleanDialog(anchor: android.view.View) {
        val glView = fragment.glView ?: return
        glView.queueEvent {
            val renderer = glView.renderer
            val model = renderer.model ?: return@queueEvent
            val selected = renderer.selectedObjectIndices()
            if (selected.size < 2) return@queueEvent
            val targetIndex = renderer.selectedObject.takeIf { it in selected } ?: selected[0]
            val orderedIndices = intArrayOf(targetIndex) + selected.filter { it != targetIndex }
            val targetName = model.getObjectName(targetIndex)
            val toolNames = orderedIndices.drop(1).map { index -> index to model.getObjectName(index) }
            val sourceRevision = model.geometryRevision
            val sourceObjectCount = model.objectsCount

            ViewUtils.postOnMainThread {
                if (!fragment.canShowWorkspaceComposeDialog()) return@postOnMainThread
                val displayTargetName = targetName
                    ?: anchor.context.getString(R.string.MenuFileObjectNumber, targetIndex + 1)
                val displayToolNames = toolNames.map { (index, name) ->
                    name ?: anchor.context.getString(R.string.MenuFileObjectNumber, index + 1)
                }
                fragment.showWorkspaceComposeDialog { dismiss ->
                    ModelBooleanDialog(
                        targetName = displayTargetName,
                        toolNames = displayToolNames,
                        onDismiss = dismiss,
                        onChoose = { operation ->
                            dismiss()
                            glView.queueEvent {
                                val currentRenderer = glView.renderer
                                val currentModel = currentRenderer.model
                                val selectionUnchanged = currentModel != null && currentModel === model &&
                                    currentModel.objectsCount == sourceObjectCount &&
                                    currentModel.geometryRevision == sourceRevision &&
                                    currentRenderer.selectedObject == targetIndex &&
                                    currentRenderer.selectedObjectIndices().contentEquals(selected)
                                if (!selectionUnchanged) {
                                    ViewUtils.postOnMainThread {
                                        SliceBeam.EVENT_BUS.fireEvent(
                                            NeedSnackbarEvent(R.string.ModelBooleanSelectionChanged),
                                        )
                                    }
                                    return@queueEvent
                                }
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
                                        val specificMessage = ModelBooleanFailureMessage.resourceFor(result)
                                        val message = specificMessage ?: R.string.ModelBooleanFailed
                                        SliceBeam.EVENT_BUS.fireEvent(
                                            if (specificMessage == null) {
                                                NeedSnackbarEvent(message, displayTargetName, displayToolNames.joinToString())
                                            } else {
                                                NeedSnackbarEvent(message)
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
