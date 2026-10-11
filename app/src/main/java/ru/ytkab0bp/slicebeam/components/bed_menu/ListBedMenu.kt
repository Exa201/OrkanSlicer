package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import androidx.annotation.CallSuper
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.fragment.BedFragment

abstract class ListBedMenu : BedMenu() {
    @JvmField
    protected var fragment: BedFragment? = null

    override fun onSetBed(fragment: BedFragment?) {
        this.fragment = fragment
    }

    @CallSuper
    override fun onViewCreated(v: View) {
        super.onViewCreated(v)
        SliceBeam.EVENT_BUS.registerListener(this)
    }

    @CallSuper
    override fun onViewDestroyed() {
        super.onViewDestroyed()
        SliceBeam.EVENT_BUS.unregisterListener(this)
    }

    protected abstract fun onCreateItems(portrait: Boolean): List<BedMenuItem>
}
