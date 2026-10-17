package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.FlattenModeResetEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent

/** Model selection, placement, duplication and object-list actions. */
class ModelMenu : ListBedMenu() {
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var menuItems: ModelMenuItems? = null

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> = emptyList()

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        val bed = fragment ?: return
        val items = remember(this, bed) { ModelMenuItems(bed, ::refreshActionItems) }
        LaunchedEffect(items) {
            menuItems = items
            refreshActionItems()
        }
        BedMenuActionsContent(composeActions.value, portrait, multiRow = true)
    }

    @EventHandler(runOnMainThread = true)
    fun onObjectsChanged(event: ObjectsListChangedEvent) {
        menuItems?.resetFlattenMode()
        refreshActionItems()
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectionChanged(event: SelectedObjectChangedEvent) {
        menuItems?.resetFlattenMode()
        refreshActionItems()
    }

    @EventHandler(runOnMainThread = true)
    fun onFlattenModeReset(event: FlattenModeResetEvent) {
        menuItems?.resetFlattenMode()
        refreshActionItems()
    }

    private fun refreshActionItems() {
        composeActions.value = menuItems?.createItems().orEmpty()
    }
}
