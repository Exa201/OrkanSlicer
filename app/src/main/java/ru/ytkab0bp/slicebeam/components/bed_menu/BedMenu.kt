package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import ru.ytkab0bp.slicebeam.fragment.BedFragment

abstract class BedMenu {
    private var storedView: View? = null

    abstract fun onCreateView(ctx: Context, portrait: Boolean): View

    @CallSuper
    open fun onViewCreated(v: View) {
        storedView = v
    }

    @CallSuper
    open fun onViewDestroyed() {
        storedView = null
    }

    open fun getView(): View? = storedView

    open fun onSetBed(fragment: BedFragment?) = Unit
}
