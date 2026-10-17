package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.FlattenModeResetEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import java.util.function.Consumer

class OrientationMenu : ListBedMenu() {
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var menuItems: OrientationMenuItems? = null

    override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView {
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        val bed = fragment
        if (bed != null) {
            val items = OrientationMenuItems(
                bed,
                Consumer<View> { bed.showWorkspaceComposeTool(PositionMenu()) },
                Consumer<View> { bed.showWorkspaceComposeTool(RotationMenu()) },
                ::refreshActionItems,
            )
            menuItems = items
            composeActions.value = items.createItems()
        }
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                BedMenuActionsContent(composeActions.value, portrait)
            }
        }
        return themedView
    }

    override fun onCreateItems(portrait: Boolean): List<BedMenuItem> = emptyList()

    @Composable
    override fun WorkspaceContent(portrait: Boolean) {
        val bed = fragment ?: return
        val items = remember(this, bed) {
            OrientationMenuItems(
                bed,
                Consumer<View> { bed.showWorkspaceComposeTool(PositionMenu()) },
                Consumer<View> { bed.showWorkspaceComposeTool(RotationMenu()) },
                ::refreshActionItems,
            )
        }
        LaunchedEffect(items) {
            menuItems = items
            composeActions.value = items.createItems()
        }
        BedMenuActionsContent(composeActions.value, portrait)
    }

    @EventHandler(runOnMainThread = true)
    fun onFlattenModeReset(event: FlattenModeResetEvent) {
        menuItems?.onFlattenModeReset()
        refreshActionItems()
    }

    @EventHandler(runOnMainThread = true)
    fun onObjectsChanged(event: ObjectsListChangedEvent) {
        menuItems?.onObjectsChanged()
        refreshActionItems()
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectionChanged(event: SelectedObjectChangedEvent) {
        menuItems?.onSelectionChanged()
        refreshActionItems()
    }

    private fun refreshActionItems() {
        composeActions.value = menuItems?.createItems().orEmpty()
    }

    /** Keep Java callers source-compatible with menu.new PositionMenu(). */
    inner class PositionMenu : OrientationPositionMenu(requireNotNull(this@OrientationMenu.fragment))

    /** Keep Java callers source-compatible with menu.new RotationMenu(). */
    inner class RotationMenu : OrientationRotationMenu(requireNotNull(this@OrientationMenu.fragment))
}
