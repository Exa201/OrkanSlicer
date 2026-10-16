package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.fragment.BedFragment

abstract class ListBedMenu : BedMenu(), WorkspaceComposeBedMenu {
    @JvmField
    protected var fragment: BedFragment? = null

    private var workspaceEventsRegistered = false

    @Composable
    abstract override fun WorkspaceContent(portrait: Boolean)

    override fun onSetBed(fragment: BedFragment?) {
        this.fragment = fragment
    }

    @CallSuper
    override fun onViewCreated(v: View) {
        super.onViewCreated(v)
        registerWorkspaceEvents()
    }

    @CallSuper
    override fun onViewDestroyed() {
        super.onViewDestroyed()
        unregisterWorkspaceEvents()
    }

    override fun onWorkspaceMenuVisible() = registerWorkspaceEvents()

    override fun onWorkspaceMenuHidden() = unregisterWorkspaceEvents()

    private fun registerWorkspaceEvents() {
        if (workspaceEventsRegistered) return
        SliceBeam.EVENT_BUS.registerListener(this)
        workspaceEventsRegistered = true
    }

    private fun unregisterWorkspaceEvents() {
        if (!workspaceEventsRegistered) return
        SliceBeam.EVENT_BUS.unregisterListener(this)
        workspaceEventsRegistered = false
    }

    protected abstract fun onCreateItems(portrait: Boolean): List<BedMenuItem>
}
