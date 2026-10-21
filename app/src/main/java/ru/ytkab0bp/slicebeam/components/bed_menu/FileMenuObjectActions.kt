package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import android.widget.Toast
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.Locale

/** Kotlin-owned object selection, duplication, filament and plate actions for the live File menu. */
class FileMenuObjectActions(private val fragment: BedFragment) {
    private fun hasSelection(): Boolean =
        fragment.glView?.renderer?.let { it.selectedObject != -1 && it.model != null } == true

    fun showCopyMoveDialog(view: View) {
        val context = view.context
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = context.getString(R.string.MenuFileCopyMove),
                cancelLabel = context.getString(android.R.string.cancel),
                choices = listOf(
                    context.getString(R.string.MenuFileDuplicateCount),
                    context.getString(R.string.MenuFileMoveToPlate),
                ),
                onDismiss = dismiss,
                onSelect = { which ->
                    dismiss()
                    if (which == 0) showDuplicateDialog(view) else showMoveToPlateDialog(view)
                },
            )
        }
    }

    fun showObjectsDialog(view: View) {
        val model = fragment.currentModel
        val count = model?.objectsCount ?: 0
        if (count == 0) {
            Toast.makeText(view.context, R.string.MenuFileObjectsEmpty, Toast.LENGTH_SHORT).show()
            return
        }

        val context = view.context
        val selected = fragment.glView?.renderer?.selectedObject ?: -1
        val filamentSlots = Prefs.getFilamentSlots()
        val rows = (0 until count).map { index ->
            val name = model?.getObjectName(index)
                ?: context.getString(R.string.MenuFileObjectNumber, index + 1)
            val tool = (model?.getExtruder(index) ?: 1).minus(1).coerceAtLeast(0)
            val slot = filamentSlots.getOrNull(tool)
            ModelObjectRow(
                name = name,
                tool = tool,
                material = slot?.type?.takeIf(String::isNotBlank) ?: context.getString(R.string.FilamentType),
                color = slot?.color ?: 0xFF888888.toInt(),
                selected = index == selected,
                assignEnabled = filamentSlots.isNotEmpty(),
            )
        }
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelObjectsDialog(
                title = context.getString(R.string.MenuFileObjects),
                cancelLabel = context.getString(android.R.string.cancel),
                assignLabel = context.getString(R.string.ObjectActionAssignFilamentShort),
                rows = rows,
                onDismiss = dismiss,
                onSelect = { index ->
                    dismiss()
                    fragment.glView?.selectModel(index) {
                        SliceBeam.EVENT_BUS.fireEvent(SelectedObjectChangedEvent())
                    }
                },
                onAssign = { index ->
                    dismiss()
                    showObjectFilamentSlotDialog(view, index)
                },
            )
        }
    }

    fun showSelectedObjectActions(view: View) {
        if (!hasSelection()) return
        val context = view.context
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = context.getString(R.string.MenuFileObjectActions),
                cancelLabel = context.getString(android.R.string.cancel),
                choices = listOf(
                    context.getString(R.string.MenuFileDuplicateCount),
                    context.getString(R.string.MenuFileMoveToPlate),
                    context.getString(R.string.MenuToolbarSplit),
                    context.getString(R.string.MenuFileDelete),
                ),
                onDismiss = dismiss,
                onSelect = { which ->
                    dismiss()
                    when (which) {
                        0 -> showDuplicateDialog(view)
                        1 -> showMoveToPlateDialog(view)
                        2 -> fragment.splitSelectedObject()
                        3 -> fragment.glView?.deleteSelectedModel(fragment::updateModel)
                    }
                },
            )
        }
    }

    /** Assigns a whole object to an ACE/slicer tool; fine-grained color painting stays separate. */
    fun showObjectFilamentSlotDialog(view: View) {
        if (!hasSelection()) return
        val selected = fragment.glView?.renderer?.selectedObject ?: return
        showObjectFilamentSlotDialog(view, selected)
    }

    private fun showObjectFilamentSlotDialog(view: View, objectIndex: Int) {
        val slots: List<FilamentSlot> = Prefs.getFilamentSlots()
        if (slots.isEmpty()) return
        val model = fragment.currentModel ?: return
        if (objectIndex !in 0 until model.objectsCount) return

        val context = view.context
        val currentTool = model.getExtruder(objectIndex) - 1
        // Match the complete persisted Slicer palette. Limiting this picker to four made
        // individually loaded objects impossible to assign to ACE Pro / larger tool palettes.
        val choices = slots.indices.map { tool ->
            val slot = slots[tool]
            val label = context.getString(
                R.string.ModelContextFilamentSlot,
                tool,
                slot.type,
                String.format(Locale.ROOT, "#%06X", slot.color and 0xFFFFFF),
            )
            if (tool == currentTool) "✓ $label" else label
        }
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = context.getString(R.string.ModelContextSelectFilamentSlot),
                cancelLabel = context.getString(android.R.string.cancel),
                choices = choices,
                onDismiss = dismiss,
                onSelect = { which ->
                    dismiss()
                    val viewport = fragment.glView ?: return@ModelChoiceDialog
                    // Slicer extruders are one-based: T0 always maps to physical/UI slot 1.
                    val filament = which + 1
                    viewport.queueEvent {
                        // An explicit object assignment must replace 3MF per-volume and paint
                        // assignments too; changing only the object's base extruder leaves those
                        // imported material overrides active during preview and slicing.
                        viewport.renderer.setObjectUniformFilament(objectIndex, filament)
                        viewport.requestRender()
                        ViewUtils.postOnMainThread {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ModelContextFilamentSlotApplied, which),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
            )
        }
    }

    fun showDuplicateDialog(view: View) {
        if (!hasSelection()) return
        val context = view.context
        fragment.showWorkspaceComposeDialog { dismiss ->
            DuplicateModelDialog(
                title = context.getString(R.string.MenuFileDuplicateCount),
                hint = context.getString(R.string.MenuFileDuplicateCountHint),
                confirmLabel = context.getString(R.string.ModelContextDuplicate),
                cancelLabel = context.getString(android.R.string.cancel),
                onDismiss = dismiss,
                onConfirm = { copies ->
                    dismiss()
                    fragment.glView?.duplicateSelectedModel(copies) {
                        fragment.updateModel()
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(R.plurals.MenuFileDuplicatedCount, copies, copies),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        }
    }

    fun showMoveToPlateDialog(view: View) {
        if (!hasSelection()) return
        val plateCount = fragment.plateCount
        if (plateCount < 2) {
            Toast.makeText(view.context, R.string.MenuFileMoveNoOtherPlate, Toast.LENGTH_SHORT).show()
            return
        }

        val context = view.context
        val source = fragment.currentPlateIndex
        val targets = (0 until plateCount).filter { it != source }
        val labels = targets.map { context.getString(R.string.PlateNumber, (it + 1).toString()) }
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelChoiceDialog(
                title = context.getString(R.string.MenuFileMoveToPlate),
                cancelLabel = context.getString(android.R.string.cancel),
                choices = labels,
                onDismiss = dismiss,
                onSelect = { which ->
                    dismiss()
                    val target = targets.getOrNull(which) ?: return@ModelChoiceDialog
                    if (target !in 0 until fragment.plateCount || target == fragment.currentPlateIndex) return@ModelChoiceDialog
                    fragment.moveSelectedObjectToPlate(target) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.MenuFileMovedToPlate, (target + 1).toString()),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        }
    }
}
