package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import java.util.function.Consumer

/** Compose edit-menu host; selection-dependent action state follows model events. */
class TransformMenu : ListBedMenu() {
    private val composeActions = mutableStateOf<List<BedComposeMenuAction>>(emptyList())
    private var menuItems: TransformMenuItems? = null

    override fun onCreateView(ctx: Context, portrait: Boolean): ThemeAwareComposeView {
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        val bed = fragment
        if (bed != null) {
            val items = createMenuItems(bed)
            menuItems = items
            composeActions.value = items.createItems()
        }
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                BedMenuActionsContent(composeActions.value, portrait, multiRow = true)
            }
        }
        return themedView
    }

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
        Consumer<View> { view -> bed.showUnfoldMenu(OrientationPositionMenu(bed), view) },
        Consumer<View> { view -> bed.showUnfoldMenu(OrientationRotationMenu(bed), view) },
        Consumer<View> { view -> bed.showUnfoldMenu(TransformScaleMenu(bed), view) },
        Consumer<View> { view -> bed.showUnfoldMenu(TransformCutMenu(bed), view) },
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
