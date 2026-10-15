package ru.ytkab0bp.slicebeam.components.bed_menu

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.roundToInt

/** Builds Compose actions for the edit menu and retains every model operation and submenu route. */
internal class TransformMenuItems(
    private val fragment: BedFragment,
    private val showPositionMenu: Consumer<View>,
    private val showRotationMenu: Consumer<View>,
    private val showScaleMenu: Consumer<View>,
    private val showCutMenu: Consumer<View>,
) {
    fun createItems(): List<BedComposeMenuAction> {
        val renderer = fragment.glView!!.renderer
        val availability = modelSelectionAvailability(renderer.model != null, renderer.selectedObject)
        return listOf(
            BedComposeMenuAction(R.string.WorkspaceObjects, R.drawable.square_stack_up_outline_28, {
                FileMenuObjectActions(fragment).showObjectsDialog(it)
            }, enabled = (renderer.model?.objectsCount ?: 0) > 0),
            selectionAction(R.string.ModelContextResetRotation, R.drawable.menu_orientation_rotation_28, availability) {
                fragment.glView!!.queueEvent {
                    renderer.setSelectionRotation(0.0, 0.0, 0.0)
                    renderer.invalidateSelectionObject()
                    fragment.glView!!.requestRender()
                    ViewUtils.postOnMainThread {
                        fragment.updateModel()
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelContextResetRotationDone))
                    }
                }
            },
            selectionAction(R.string.ModelContextResetScale, R.drawable.arrow_up_right_corner_outline_24, availability) {
                fragment.glView!!.queueEvent {
                    renderer.setSelectionScale(1.0, 1.0, 1.0)
                    renderer.invalidateSelectionObject()
                    fragment.glView!!.requestRender()
                    ViewUtils.postOnMainThread {
                        fragment.updateModel()
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelContextResetScaleDone))
                    }
                }
            },
            BedComposeMenuAction(R.string.ModelUndo, R.drawable.arrow_left_outline_28, {
                fragment.undoModelEdit()
            }),
            BedComposeMenuAction(R.string.ModelRedo, R.drawable.refresh_outline_28, {
                fragment.redoModelEdit()
            }),
            selectionAction(R.string.MenuToolbarMove, R.drawable.menu_orientation_position_28, availability) {
                showPositionMenu.accept(it)
            },
            selectionAction(R.string.MenuToolbarRotate, R.drawable.menu_orientation_rotation_28, availability) {
                showRotationMenu.accept(it)
            },
            selectionAction(R.string.MenuTransformScale, R.drawable.arrow_up_right_corner_outline_24, availability) {
                showScaleMenu.accept(it)
            },
            selectionAction(R.string.ModelSimplify, R.drawable.sliders_outline_28, availability, ::showSimplifyDialog),
            selectionAction(R.string.MenuTransformCut, R.drawable.menu_transform_cut_or_mirror_28, availability) {
                showCutMenu.accept(it)
            },
            selectionAction(R.string.MenuToolbarSplit, R.drawable.menu_transform_cut_or_mirror_28, availability) {
                fragment.splitSelectedObject()
            },
            selectionAction(R.string.MenuToolbarVariableLayerHeight, R.drawable.sliders_outline_28, availability) {
                fragment.enterVariableLayerHeightMode()
            },
            paintAction(R.string.MenuToolbarSupportPaint, GLRenderer.PAINT_MODE_SUPPORT, availability),
            paintAction(R.string.MenuToolbarSeamPaint, GLRenderer.PAINT_MODE_SEAM, availability),
            paintAction(R.string.MenuToolbarFuzzyPaint, GLRenderer.PAINT_MODE_FUZZY, availability),
            paintAction(R.string.MenuToolbarColorPaint, GLRenderer.PAINT_MODE_COLOR, availability),
            selectionAction(R.string.MenuToolbarEmboss, R.drawable.edit_outline_28, availability) {
                fragment.enterEmbossMode()
            },
            BedComposeMenuAction(R.string.MenuToolbarMeasure, R.drawable.wrench_outline_28, {
                fragment.enterMeasureMode()
            }),
        )
    }

    private fun selectionAction(
        titleRes: Int,
        iconRes: Int,
        availability: ModelSelectionAvailability,
        action: (View) -> Unit,
    ): BedComposeMenuAction = BedComposeMenuAction(
        titleRes = titleRes,
        iconRes = iconRes,
        onClick = action,
        enabled = availability.hasSelectedObject,
    )

    private fun paintAction(titleRes: Int, mode: Int, availability: ModelSelectionAvailability) =
        selectionAction(titleRes, R.drawable.paint_roller_outline_28, availability) {
            val objectIndex = fragment.glView!!.renderer.selectedObject
            if (objectIndex != -1) fragment.enterPaintMode(objectIndex, mode)
        }

    private fun showSimplifyDialog(view: View) {
        val triangles = fragment.selectedTriangleCount
        if (triangles <= 4) return
        val context = view.context
        val ratios = floatArrayOf(0.75f, 0.50f, 0.25f)
        val targetCounts = ratios.map { max(4, (triangles * it).roundToInt()) }
        val choices = arrayOf<CharSequence>(
            context.resources.getQuantityString(R.plurals.ModelSimplifyLight, targetCounts[0], targetCounts[0]),
            context.resources.getQuantityString(R.plurals.ModelSimplifyMedium, targetCounts[1], targetCounts[1]),
            context.resources.getQuantityString(R.plurals.ModelSimplifyStrong, targetCounts[2], targetCounts[2]),
            context.getString(R.string.ModelSimplifyCustom),
        )
        val selected = AtomicInteger(1)
        BeamAlertDialogBuilder(context)
            .setTitle(R.string.ModelSimplify)
            .setMessage(context.resources.getQuantityString(R.plurals.ModelSimplifyCurrent, triangles, triangles))
            .setSingleChoiceItems(choices, selected.get()) { _, which -> selected.set(which) }
            .setPositiveButton(R.string.ModelSimplifyStart) { _, _ ->
                val choice = selected.get()
                if (choice < ratios.size) {
                    fragment.simplifySelectedObject(ratios[choice])
                } else {
                    showCustomSimplifyDialog(view, triangles)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomSimplifyDialog(view: View, triangles: Int) {
        val input = EditText(view.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(max(4, triangles / 2).toString())
            setSelectAllOnFocus(true)
        }
        val padding = ViewUtils.dp(24)
        val container = FrameLayout(view.context).apply {
            setPadding(padding, 0, padding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        BeamAlertDialogBuilder(view.context)
            .setTitle(R.string.ModelSimplifyCustom)
            .setMessage(R.string.ModelSimplifyCustomHint)
            .setView(container)
            .setPositiveButton(R.string.ModelSimplifyStart) { _, _ ->
                val target = (input.text.toString().toIntOrNull() ?: triangles / 2)
                    .coerceIn(4, triangles - 1)
                fragment.simplifySelectedObject(target.toFloat() / triangles.toFloat())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
