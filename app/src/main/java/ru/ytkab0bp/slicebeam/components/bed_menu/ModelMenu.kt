package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.FlattenModeResetEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView

/** Model selection, placement, duplication and object-list actions. */
class ModelMenu : ListBedMenu() {
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var menuItems: ModelMenuItems? = null

    override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView {
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        fragment?.let { bed ->
            menuItems = ModelMenuItems(bed, ::refreshActionItems)
            refreshActionItems()
        }
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                BedMenuActionsContent(composeActions.value, portrait, multiRow = true)
            }
        }
        return themedView
    }

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> = emptyList()

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
