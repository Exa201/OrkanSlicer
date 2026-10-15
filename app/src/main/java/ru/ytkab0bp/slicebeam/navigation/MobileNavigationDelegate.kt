package ru.ytkab0bp.slicebeam.navigation

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveNavigationPolicy
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveWorkspaceNavigation
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceNavigationItem
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Adaptive navigation modeled on SliceBeam's Preview, Print, Filament, Printer, and App areas. */
class MobileNavigationDelegate : DelegateSlotImpl() {
    private var portrait = false
    private var bottomNavigation = false
    private var root: FrameLayout? = null
    private var rootLayout: LinearLayout? = null
    private var navigationView: ComposeView? = null
    private var lastPrimarySlot = 0
    private var selectedNavigationSlot by mutableIntStateOf(0)
    private var navigationThemeRevision by mutableIntStateOf(0)

    fun switchToSlot(slot: Int) {
        if (slot !in 0 until getSlotCount()) return
        if (slot != SETTINGS_SLOT) {
            lastPrimarySlot = slot
        } else if (currentSlot != SETTINGS_SLOT) {
            lastPrimarySlot = currentSlot
        }
        switchSlot(slot) { updateSelectedNavigationSlot(slot) }
    }

    override fun onApplyTheme() {
        super.onApplyTheme()
        navigationThemeRevision++
        root?.setBackgroundColor(ThemesRepo.getColor(android.R.attr.windowBackground))
    }

    override fun onCreateView(ctx: Context): View {
        val frame = FrameLayout(ctx)
        val navigationLayout = LinearLayout(ctx)
        val configuration = ctx.resources.configuration
        portrait = AdaptiveNavigationPolicy.isPortraitWindow(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
        )
        bottomNavigation = AdaptiveNavigationPolicy.usesBottomNavigation(
            configuration.screenWidthDp,
            portrait,
        )
        selectedNavigationSlot = MobileNavigationPolicy.selectedDestination(currentSlot)
        rootLayout = navigationLayout
        navigationLayout.orientation = if (bottomNavigation) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        if (bottomNavigation) {
            val bottomBar = createNavigationHost(ctx)
            navigationView = bottomBar
            navigationLayout.addView(bottomBar, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        }

        val content = FrameLayout(ctx)
        container = content
        val fragment = getCurrentFragment()
        val fragmentView = fragment.getView() ?: fragment.onCreateView(ctx).also {
            fragment.onViewCreated(it)
            fragment.onApplyTheme()
        }
        content.addView(fragmentView)
        navigationLayout.addView(content, LinearLayout.LayoutParams(
            if (bottomNavigation) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            if (bottomNavigation) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            1f,
        ))

        if (!bottomNavigation) {
            val rail = createNavigationHost(ctx)
            navigationView = rail
            navigationLayout.addView(rail, LinearLayout.LayoutParams(
                ViewUtils.dp(AdaptiveNavigationPolicy.NAVIGATION_RAIL_WIDTH_DP),
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
        }
        frame.setBackgroundColor(ThemesRepo.getColor(android.R.attr.windowBackground))
        frame.addView(
            navigationLayout,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        root = frame
        return frame
    }

    private fun updateSelectedNavigationSlot(slot: Int) {
        val destination = MobileNavigationPolicy.selectedDestination(slot)
        if (MobileNavigationPolicy.isDestination(destination)) {
            selectedNavigationSlot = destination
        }
    }

    private fun createNavigationHost(ctx: Context): ComposeView {
        val items = MobileNavigationPolicy.destinations().map { slot ->
            WorkspaceNavigationItem(
                slot = slot,
                label = ctx.getString(getSlotNavigationTitle(slot)),
                icon = getSlotIcon(slot),
                accessibilityLabel = ctx.getString(getSlotTooltip(slot)),
                compactLabel = if (slot == MobileNavigationPolicy.PREVIEW_SLOT) ctx.getString(R.string.NavigationPreviewCompact) else null,
            )
        }
        return ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                key(navigationThemeRevision) {
                    BeamComposeTheme {
                        AdaptiveWorkspaceNavigation(
                            items = items,
                            selectedSlot = selectedNavigationSlot,
                            isBottomNavigation = bottomNavigation,
                            onSelected = ::onNavigationItemSelected,
                        )
                    }
                }
            }
        }
    }

    private fun onNavigationItemSelected(slot: Int) {
        if (!MobileNavigationPolicy.isDestination(slot) || slot == currentSlot) return
        if (slot != SETTINGS_SLOT) lastPrimarySlot = slot
        else if (currentSlot != SETTINGS_SLOT) lastPrimarySlot = currentSlot
        switchSlot(slot) {
            updateSelectedNavigationSlot(slot)
        }
    }

    fun onConfigurationChanged(configuration: Configuration) {
        portrait = AdaptiveNavigationPolicy.isPortraitWindow(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
        )
        val useBottomNavigation = AdaptiveNavigationPolicy.usesBottomNavigation(
            configuration.screenWidthDp,
            portrait,
        )
        if (useBottomNavigation == bottomNavigation) return
        val layout = rootLayout ?: return
        val navigation = navigationView ?: return
        val ctx = context ?: return

        // Keep the active fragment attached while changing the navigation shape. This preserves
        // overlays and WebViews inside the current screen during rotation and window resizing.
        layout.removeView(navigation)
        bottomNavigation = useBottomNavigation
        layout.orientation = if (bottomNavigation) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val replacement = createNavigationHost(ctx)
        navigationView = replacement
        val content = container
        if (bottomNavigation) {
            layout.addView(replacement, 0, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
            content?.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        } else {
            content?.layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f,
            )
            layout.addView(replacement, LinearLayout.LayoutParams(
                ViewUtils.dp(AdaptiveNavigationPolicy.NAVIGATION_RAIL_WIDTH_DP),
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
        }
    }

    override fun onBackPressed(): Boolean {
        if (super.onBackPressed()) return true
        if (currentSlot == SETTINGS_SLOT) {
            val target = lastPrimarySlot.takeIf { it in 0 until SETTINGS_SLOT } ?: 0
            switchSlot(target) {
                updateSelectedNavigationSlot(target)
            }
            return true
        }
        if (currentSlot != 0) {
            switchSlot(0) {
                lastPrimarySlot = 0
                updateSelectedNavigationSlot(0)
            }
            return true
        }
        return false
    }

    override fun getOverlayView(): FrameLayout? = root

    override fun isSwitchingWithX(): Boolean = portrait

    private companion object {
        const val SETTINGS_SLOT = 5
    }
}
