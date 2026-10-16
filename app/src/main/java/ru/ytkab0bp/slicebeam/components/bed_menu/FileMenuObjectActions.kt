package ru.ytkab0bp.slicebeam.components.bed_menu

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.Locale

/** Kotlin-owned object selection, duplication, filament and plate actions for the live File menu. */
class FileMenuObjectActions(private val fragment: BedFragment) {
    private fun hasSelection(): Boolean =
        fragment.glView!!.renderer.selectedObject != -1 && fragment.glView!!.renderer.model != null

    fun showCopyMoveDialog(view: View) {
        val context = view.context
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.MenuFileCopyMove)
            .setItems(
                arrayOf<CharSequence>(
                    context.getString(R.string.MenuFileDuplicateCount),
                    context.getString(R.string.MenuFileMoveToPlate),
                ),
            ) { _, which ->
                if (which == 0) showDuplicateDialog(view) else showMoveToPlateDialog(view)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showObjectsDialog(view: View) {
        val model = fragment.currentModel
        val count = model?.objectsCount ?: 0
        if (count == 0) {
            Toast.makeText(view.context, R.string.MenuFileObjectsEmpty, Toast.LENGTH_SHORT).show()
            return
        }

        val context = view.context
        val selected = fragment.glView!!.renderer.selectedObject
        val filamentSlots = Prefs.getFilamentSlots()
        lateinit var objectListDialog: AlertDialog
        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ViewUtils.dp(16), ViewUtils.dp(4), ViewUtils.dp(16), ViewUtils.dp(4))
        }
        for (index in 0 until count) {
            val objectName = model?.getObjectName(index)
                ?: context.getString(R.string.MenuFileObjectNumber, index + 1)
            val active = index == selected
            val tool = (model?.getExtruder(index) ?: 1).minus(1).coerceAtLeast(0)
            val slot = filamentSlots.getOrNull(tool)
            val colorText = slot?.color?.let { String.format(Locale.ROOT, "#%06X", it and 0xFFFFFF) } ?: ""
            val materialText = slot?.type?.takeIf(String::isNotBlank) ?: context.getString(R.string.FilamentType)
            val objectRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(4), ViewUtils.dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = ViewUtils.dp(10).toFloat()
                    val highlight = ThemesRepo.getColor(android.R.attr.colorControlHighlight)
                    setColor(if (active) androidx.core.graphics.ColorUtils.setAlphaComponent(highlight, 36) else Color.TRANSPARENT)
                }
                isClickable = true
                isFocusable = true
                contentDescription = context.getString(R.string.MenuFileObjectWithFilament,
                    objectName, tool, materialText, colorText)
                setOnClickListener {
                    objectListDialog.dismiss()
                    fragment.glView!!.selectModel(index) {
                        SliceBeam.EVENT_BUS.fireEvent(SelectedObjectChangedEvent())
                    }
                }
            }
            val colorIndicator = View(context).apply {
                val color = slot?.color ?: ThemesRepo.getColor(android.R.attr.textColorSecondary)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            objectRow.addView(colorIndicator, LinearLayout.LayoutParams(ViewUtils.dp(18), ViewUtils.dp(18)).apply {
                marginEnd = ViewUtils.dp(10)
            })
            val label = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            label.addView(TextView(context).apply {
                text = objectName
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            label.addView(TextView(context).apply {
                text = context.getString(R.string.ModelContextFilamentSlot, tool, materialText, colorText)
                textSize = 12f
                setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            objectRow.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val assign = TextView(context).apply {
                text = context.getString(R.string.ObjectActionAssignFilamentShort)
                textSize = 12f
                setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent))
                gravity = Gravity.CENTER
                setPadding(ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8), ViewUtils.dp(8))
                minWidth = ViewUtils.dp(56)
                minHeight = ViewUtils.dp(48)
                isEnabled = filamentSlots.isNotEmpty()
                isClickable = true
                isFocusable = true
                contentDescription = context.getString(R.string.ObjectActionAssignFilament, objectName)
                setOnClickListener { showObjectFilamentSlotDialog(view, index) }
            }
            objectRow.addView(assign)
            rows.addView(objectRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(72)).apply {
                bottomMargin = ViewUtils.dp(4)
            })
        }
        objectListDialog = BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.MenuFileObjects)
            .setView(ScrollView(context).apply {
                isFillViewport = true
                addView(rows, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        objectListDialog.show()
    }

    fun showSelectedObjectActions(view: View) {
        if (!hasSelection()) return
        val context = view.context
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.MenuFileObjectActions)
            .setItems(
                arrayOf<CharSequence>(
                    context.getString(R.string.MenuFileDuplicateCount),
                    context.getString(R.string.MenuFileMoveToPlate),
                    context.getString(R.string.MenuToolbarSplit),
                    context.getString(R.string.MenuFileDelete),
                ),
            ) { _, which ->
                when (which) {
                    0 -> showDuplicateDialog(view)
                    1 -> showMoveToPlateDialog(view)
                    2 -> fragment.splitSelectedObject()
                    3 -> fragment.glView!!.deleteSelectedModel(fragment::updateModel)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Assigns a whole object to an ACE/slicer tool; fine-grained color painting stays separate. */
    fun showObjectFilamentSlotDialog(view: View) {
        if (!hasSelection()) return

        showObjectFilamentSlotDialog(view, fragment.glView!!.renderer.selectedObject)
    }

    private fun showObjectFilamentSlotDialog(view: View, objectIndex: Int) {
        val slots: List<FilamentSlot> = Prefs.getFilamentSlots()
        if (slots.isEmpty()) return

        val model = fragment.currentModel ?: return
        val choices = Array<CharSequence>(minOf(4, slots.size)) { "" }
        val currentTool = model.getExtruder(objectIndex) - 1
        for (tool in choices.indices) {
            val slot = slots[tool]
            val label = view.context.getString(
                R.string.ModelContextFilamentSlot,
                tool,
                slot.type,
                String.format(Locale.ROOT, "#%06X", slot.color and 0xFFFFFF),
            )
            choices[tool] = if (tool == currentTool) "✓ $label" else label
        }

        BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.ModelContextSelectFilamentSlot)
            .setItems(choices) { _, which ->
                val filament = which + 1 // libSlic3r object extruders are one-based.
                fragment.glView!!.queueEvent {
                    fragment.glView!!.renderer.setObjectBaseFilament(objectIndex, filament)
                    fragment.glView!!.requestRender()
                    ViewUtils.postOnMainThread {
                        Toast.makeText(
                            view.context,
                            view.context.getString(R.string.ModelContextFilamentSlotApplied, which),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showDuplicateDialog(view: View) {
        val input = EditText(view.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
            setSelectAllOnFocus(true)
        }
        val padding = ViewUtils.dp(24)
        val container = FrameLayout(view.context).apply {
            setPadding(padding, 0, padding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

        BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.MenuFileDuplicateCount)
            .setMessage(R.string.MenuFileDuplicateCountHint)
            .setView(container)
            .setPositiveButton(R.string.ModelContextDuplicate) { _, _ ->
                val copies = input.text.toString().toIntOrNull()?.coerceIn(1, 50) ?: 1
                fragment.glView!!.duplicateSelectedModel(copies) {
                    fragment.updateModel()
                    Toast.makeText(
                        view.context,
                        view.context.resources.getQuantityString(R.plurals.MenuFileDuplicatedCount, copies, copies),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showMoveToPlateDialog(view: View) {
        val plateCount = fragment.plateCount
        if (plateCount < 2) {
            Toast.makeText(view.context, R.string.MenuFileMoveNoOtherPlate, Toast.LENGTH_SHORT).show()
            return
        }

        val current = fragment.currentPlateIndex
        val targets = (0 until plateCount).filter { it != current }
        val labels = targets.map { view.context.getString(R.string.PlateNumber, (it + 1).toString()) }
        BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.MenuFileMoveToPlate)
            .setItems(labels.toTypedArray()) { _, which ->
                val target = targets[which]
                fragment.moveSelectedObjectToPlate(target) {
                    Toast.makeText(
                        view.context,
                        view.context.getString(R.string.MenuFileMovedToPlate, (target + 1).toString()),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
