package ru.ytkab0bp.slicebeam.navigation

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.annotation.CallSuper
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

/** Base lifecycle for screens owned by the app's lightweight navigation stack. */
abstract class Fragment {
    private var fragmentView: View? = null
    private var fragmentContext: Context? = null

    fun getContext(): Context? = fragmentContext

    fun setContext(context: Context) {
        fragmentContext = context
    }

    @CallSuper
    open fun onCreate() = Unit

    @CallSuper
    open fun onResume() = Unit

    @CallSuper
    open fun onPause() = Unit

    @CallSuper
    open fun onConfigurationChanged(configuration: Configuration) = Unit

    @CallSuper
    open fun onDestroy() {
        fragmentView = null
    }

    open fun onApplyTheme() {
        fragmentView?.setBackgroundColor(ThemesRepo.getColor(android.R.attr.windowBackground))
    }

    abstract fun onCreateView(ctx: Context): View

    @CallSuper
    open fun onViewCreated(view: View) {
        fragmentView = view
    }

    fun getView(): View? = fragmentView

    open fun onBackPressed(): Boolean = false

    open fun onCheckDelayForSlotChange(): SlotChangeCallback? = null

    fun interface SlotChangeCallback {
        fun needDelay(callback: Runnable): Boolean
    }
}
