package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Activity
import androidx.compose.runtime.Composable
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.CableClipGenerator
import ru.ytkab0bp.slicebeam.components.CableGrommetGenerator
import ru.ytkab0bp.slicebeam.components.ImageToStlConverter
import ru.ytkab0bp.slicebeam.components.KromacutEditorMenu
import ru.ytkab0bp.slicebeam.components.PhoneStandGenerator
import ru.ytkab0bp.slicebeam.components.PlanterPotGenerator
import ru.ytkab0bp.slicebeam.components.PlantLabelSetGenerator
import ru.ytkab0bp.slicebeam.components.PowerStripTrayGenerator
import ru.ytkab0bp.slicebeam.components.TextTagGenerator
import ru.ytkab0bp.slicebeam.components.UtilityBoxGenerator
import ru.ytkab0bp.slicebeam.components.WallToolHolderGenerator
import ru.ytkab0bp.slicebeam.fragment.BedFragment

/** Home for local format conversion tools and maker shortcuts. */
class ConverterMenu : BedMenu(), WorkspaceComposeBedMenu {
    private var fragment: BedFragment? = null

    override fun onSetBed(fragment: BedFragment?) {
        this.fragment = fragment
    }

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        BedMenuActionsContent(createActions(), portrait)
    }

    private fun withActivity(action: (Activity, BedFragment) -> Unit) {
        val currentFragment = fragment ?: return
        val activity = currentFragment.getContext() as? Activity ?: return
        action(activity, currentFragment)
    }

    private fun createActions(): List<BedComposeMenuAction> = listOf(
        BedComposeMenuAction(R.string.ConverterPickImage, R.drawable.picture_outline_28, {
            withActivity { activity, current -> ImageToStlConverter.pickImage(activity, current) }
        }),
        BedComposeMenuAction(R.string.MenuFileKromacut, R.drawable.picture_stack_outline_28, { anchor ->
            fragment?.showWorkspaceComposeTool(KromacutEditorMenu())
        }),
        BedComposeMenuAction(R.string.LithophaneGeneratorMenu, R.drawable.picture_stack_outline_28, {
            withActivity { activity, _ -> ImageToStlConverter.pickLithophane(activity) }
        }),
        BedComposeMenuAction(R.string.TextTagMenu, R.drawable.edit_outline_28, {
            withActivity { activity, current -> TextTagGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.UtilityBoxMenu, R.drawable.box_heart_outline_28, {
            withActivity { activity, current -> UtilityBoxGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.CableClipMenu, R.drawable.external_link_outline_24, {
            withActivity { activity, current -> CableClipGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.CableGrommetMenu, R.drawable.external_link_outline_24, {
            withActivity { activity, current -> CableGrommetGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.WallToolHolderMenu, R.drawable.box_heart_outline_28, {
            withActivity { activity, current -> WallToolHolderGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.PlanterMenu, R.drawable.box_heart_outline_28, {
            withActivity { activity, current -> PlanterPotGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.PlantTagsMenu, R.drawable.edit_outline_28, {
            withActivity { activity, current -> PlantLabelSetGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.PhoneStandMenu, R.drawable.box_heart_outline_28, {
            withActivity { activity, current -> PhoneStandGenerator.show(activity, current) }
        }),
        BedComposeMenuAction(R.string.PowerStripMenu, R.drawable.box_heart_outline_28, {
            withActivity { activity, current -> PowerStripTrayGenerator.show(activity, current) }
        }),
    )
}
