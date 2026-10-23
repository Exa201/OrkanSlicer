package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.render.GLRenderer
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
            selectionAction(R.string.ModelContextResetRotation, R.drawable.menu_orientation_rotation_28, availability) {
                // Resolve the active object only after the operation reaches the GL queue.
                fragment.resetModelRotation {
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelContextResetRotationDone))
                }
            },
            selectionAction(R.string.ModelContextResetScale, R.drawable.arrow_up_right_corner_outline_24, availability) {
                // Resolve the active object only after the operation reaches the GL queue.
                fragment.resetModelScale {
                    SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.ModelContextResetScaleDone))
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
            fragment.enterPaintModeForCurrentSelection(mode)
        }

    private fun showSimplifyDialog(view: View) {
        val triangles = fragment.selectedTriangleCount
        if (triangles <= 4) return
        val context = view.context
        val ratios = floatArrayOf(0.75f, 0.50f, 0.25f)
        val targetCounts = ratios.map { max(4, (triangles * it).roundToInt()) }
        val choices = listOf(
            context.resources.getQuantityString(R.plurals.ModelSimplifyLight, targetCounts[0], targetCounts[0]),
            context.resources.getQuantityString(R.plurals.ModelSimplifyMedium, targetCounts[1], targetCounts[1]),
            context.resources.getQuantityString(R.plurals.ModelSimplifyStrong, targetCounts[2], targetCounts[2]),
            context.getString(R.string.ModelSimplifyCustom),
        )
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelSimplifyChoiceDialog(
                title = context.getString(R.string.ModelSimplify),
                currentTriangles = context.resources.getQuantityString(R.plurals.ModelSimplifyCurrent, triangles, triangles),
                choices = choices,
                startLabel = context.getString(R.string.ModelSimplifyStart),
                cancelLabel = context.getString(android.R.string.cancel),
                onDismiss = dismiss,
                onStart = { choice ->
                    dismiss()
                    if (choice < ratios.size) {
                        fragment.simplifySelectedObject(ratios[choice])
                    } else {
                        showCustomSimplifyDialog(view, triangles)
                    }
                },
            )
        }
    }

    private fun showCustomSimplifyDialog(view: View, triangles: Int) {
        val context = view.context
        fragment.showWorkspaceComposeDialog { dismiss ->
            ModelSimplifyCustomDialog(
                title = context.getString(R.string.ModelSimplifyCustom),
                hint = context.getString(R.string.ModelSimplifyCustomHint),
                initialTriangleCount = triangles,
                minimumTriangleCount = 4,
                maximumTriangleCount = triangles - 1,
                startLabel = context.getString(R.string.ModelSimplifyStart),
                cancelLabel = context.getString(android.R.string.cancel),
                onDismiss = dismiss,
                onStart = { target ->
                    dismiss()
                    fragment.simplifySelectedObject(target.toFloat() / triangles.toFloat())
                },
            )
        }
    }
}

/** Shared callbacks let the selected-object editor collect transforms without a second action host. */
internal fun createTransformMenuItems(fragment: BedFragment): TransformMenuItems = TransformMenuItems(
    fragment,
    Consumer<View> { fragment.showWorkspaceComposeTool(OrientationPositionMenu(fragment)) },
    Consumer<View> { fragment.showWorkspaceComposeTool(OrientationRotationMenu(fragment)) },
    Consumer<View> { fragment.showWorkspaceComposeTool(TransformScaleMenu(fragment)) },
    Consumer<View> { fragment.showWorkspaceComposeTool(TransformCutMenu(fragment)) },
)
