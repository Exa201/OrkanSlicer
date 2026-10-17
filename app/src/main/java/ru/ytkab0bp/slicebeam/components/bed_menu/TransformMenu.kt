package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import java.util.function.Consumer

/** Compose edit-menu host; selection-dependent action state follows model events. */
class TransformMenu : ListBedMenu() {
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var menuItems: TransformMenuItems? = null

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> = emptyList()

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        val bed = fragment ?: return
        val items = remember(this, bed) { createMenuItems(bed) }
        LaunchedEffect(items, portrait) {
            menuItems = items
            composeActions.value = items.createItems()
        }
        BedMenuActionsContent(composeActions.value, portrait, multiRow = true)
    }

    private fun createMenuItems(bed: ru.ytkab0bp.slicebeam.fragment.BedFragment) = TransformMenuItems(
        bed,
        Consumer<View> { bed.showWorkspaceComposeTool(OrientationPositionMenu(bed)) },
        Consumer<View> { bed.showWorkspaceComposeTool(OrientationRotationMenu(bed)) },
        Consumer<View> { bed.showWorkspaceComposeTool(TransformScaleMenu(bed)) },
        Consumer<View> { bed.showWorkspaceComposeTool(TransformCutMenu(bed)) },
    )

    @EventHandler(runOnMainThread = true)
    fun onObjectsChanged(event: ObjectsListChangedEvent) {
        refreshActionItems()
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectionChanged(event: SelectedObjectChangedEvent) {
        refreshActionItems()
    }

    private fun refreshActionItems() {
        composeActions.value = menuItems?.createItems().orEmpty()
    }
}
