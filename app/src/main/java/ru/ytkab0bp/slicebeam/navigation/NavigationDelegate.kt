package ru.ytkab0bp.slicebeam.navigation

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import java.util.Stack

/** Owns the screen stack and the animated transitions between work areas. */
abstract class NavigationDelegate {
    @JvmField
    protected var context: Context? = null

    @JvmField
    protected val fragmentStack = SparseArray<Stack<Fragment>>()

    @JvmField
    protected var currentSlot: Int = 0

    @JvmField
    protected var container: FrameLayout? = null

    private var switchAnimation: SpringAnimation? = null

    fun setContext(ctx: Context) {
        context = ctx
    }

    @CallSuper
    open fun onCreate() {
        val ctx = context ?: return
        for (index in 0 until fragmentStack.size()) {
            val fragments = fragmentStack.valueAt(index) ?: continue
            for (fragment in fragments) {
                fragment.setContext(ctx)
                fragment.onCreate()
            }
        }

        if (fragmentStack.get(currentSlot) == null) {
            val stack = Stack<Fragment>()
            fragmentStack.put(currentSlot, stack)
            val fragment = newFragment(currentSlot)
            fragment.setContext(ctx)
            fragment.onCreate()
            stack.push(fragment)
        }
    }

    open fun onApplyTheme() {
        for (index in 0 until fragmentStack.size()) {
            val stack = fragmentStack.valueAt(index) ?: continue
            for (fragment in stack) {
                fragment.onApplyTheme()
                fragment.getView()?.let { ThemesRepo.invalidateView(it) }
            }
        }
    }

    @CallSuper
    open fun onResume() {
        fragmentStack.get(currentSlot)?.peek()?.onResume()
    }

    @CallSuper
    open fun onPause() {
        fragmentStack.get(currentSlot)?.peek()?.onPause()
    }

    @CallSuper
    open fun onDestroy() {
        for (index in 0 until fragmentStack.size()) {
            val fragments = fragmentStack.valueAt(index) ?: continue
            for (fragment in fragments) fragment.onDestroy()
        }
    }

    fun getContainerView(): FrameLayout? = container

    abstract fun onCreateView(ctx: Context): View
    abstract fun newFragment(slot: Int): Fragment
    abstract fun getOverlayView(): FrameLayout?

    open fun isSwitchingWithX(): Boolean = true

    fun switchSlot(slot: Int, onInterfaceChange: Runnable) {
        val ctx = context ?: return
        val activeContainer = container ?: return
        if (slot == currentSlot && activeContainer.childCount > 0) {
            onInterfaceChange.run()
            return
        }
        var stack = fragmentStack.get(slot)
        if (stack == null) {
            stack = Stack()
            fragmentStack.put(slot, stack)
            val fragment = newFragment(slot)
            fragment.setContext(ctx)
            fragment.onCreate()
            stack.push(fragment)
        }
        val previousSlot = currentSlot
        currentSlot = slot

        if (activeContainer.childCount > 0) {
            switchAnimation?.cancel()
            val currentFragment = fragmentStack.get(previousSlot)?.peek() ?: return
            currentFragment.onPause()

            val next = Runnable {
                onInterfaceChange.run()

                val nextFragment = fragmentStack.get(slot)?.peek() ?: return@Runnable
                val previousView = activeContainer.getChildAt(0)
                val nextView = nextFragment.getView() ?: nextFragment.onCreateView(ctx).also {
                    nextFragment.onViewCreated(it)
                    nextFragment.onApplyTheme()
                }
                activeContainer.addView(nextView)

                val forward = slot > previousSlot
                val animation = SpringAnimation(FloatValueHolder(0f))
                    .setMinimumVisibleChange(1f / 256f)
                    .setSpring(SpringForce(1f)
                        .setStiffness(1000f)
                        .setDampingRatio(1f))
                    .addUpdateListener { _, value, _ ->
                        val offsetProgress = if (forward) value else 1f - value
                        if (isSwitchingWithX()) {
                            previousView.translationX = offsetProgress * -previousView.width * 0.75f
                            nextView.translationX = (1f - value) * (if (forward) 1f else -1f) * nextView.width * 0.75f
                        } else {
                            previousView.translationY = offsetProgress * -previousView.height * 0.75f
                            nextView.translationY = (1f - value) * (if (forward) 1f else -1f) * nextView.height * 0.75f
                        }
                        previousView.alpha = 1f - value
                        nextView.alpha = value
                    }
                    .addEndListener { _, _, _, _ ->
                        switchAnimation = null
                        activeContainer.removeView(previousView)
                        nextFragment.onResume()
                    }
                switchAnimation = animation
                animation.start()
            }

            val delay = currentFragment.onCheckDelayForSlotChange()
            if (delay == null || !delay.needDelay(next)) next.run()
        } else {
            val fragment = stack.peek()
            fragment.setContext(ctx)
            fragment.onCreate()
            val view = fragment.onCreateView(ctx)
            fragment.onViewCreated(view)
            fragment.onApplyTheme()
            activeContainer.addView(view)
            fragment.onResume()
            currentSlot = slot
            onInterfaceChange.run()
        }
    }

    fun getCurrentFragment(): Fragment = fragmentStack.get(currentSlot)!!.peek()

    /** Returns the retained top screen for a navigation slot without changing the active slot. */
    fun getFragmentForSlot(slot: Int): Fragment? = fragmentStack.get(slot)?.peek()

    fun destroyCurrent(): Boolean {
        val stack = fragmentStack.get(currentSlot) ?: return false
        val activeContainer = container ?: return false
        if (stack.size <= 1) return false

        val currentFragment = stack.peek()
        val previousFragment = stack[stack.size - 2]
        val previousView = activeContainer.getChildAt(0)
        val restoredView = previousFragment.getView() ?: return false
        currentFragment.onPause()
        activeContainer.addView(restoredView)

        val animation = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1f / 256f)
            .setSpring(SpringForce(1f)
                .setStiffness(1000f)
                .setDampingRatio(1f))
            .addUpdateListener { _, value, _ ->
                if (isSwitchingWithX()) {
                    previousView.translationX = value * previousView.width * 0.75f
                    restoredView.translationX = (1f - value) * -restoredView.width * 0.75f
                } else {
                    previousView.translationY = value * previousView.height * 0.75f
                    restoredView.translationY = (1f - value) * -restoredView.height * 0.75f
                }
                previousView.alpha = 1f - value
                restoredView.alpha = value
            }
            .addEndListener { _, _, _, _ ->
                switchAnimation = null
                activeContainer.removeView(previousView)
                if (stack.peek() === currentFragment) stack.pop()
                previousFragment.onResume()
                currentFragment.onDestroy()
            }
        switchAnimation = animation
        animation.start()
        return true
    }

    fun pushFragment(fragment: Fragment) {
        val ctx = context ?: return
        val activeContainer = container ?: return
        fragment.setContext(ctx)
        fragment.onCreate()

        fragmentStack.get(currentSlot)?.peek()?.onPause() ?: return
        val previousView = activeContainer.getChildAt(0)
        val newView = fragment.onCreateView(ctx)
        fragment.onViewCreated(newView)
        fragment.onApplyTheme()
        fragmentStack.get(currentSlot).push(fragment)
        activeContainer.addView(newView)

        val animation = SpringAnimation(FloatValueHolder(0f))
            .setMinimumVisibleChange(1f / 256f)
            .setSpring(SpringForce(1f)
                .setStiffness(1000f)
                .setDampingRatio(1f))
            .addUpdateListener { _, value, _ ->
                if (isSwitchingWithX()) {
                    previousView.translationX = -value * previousView.width * 0.75f
                    newView.translationX = (1f - value) * newView.width * 0.75f
                } else {
                    previousView.translationY = -value * previousView.height * 0.75f
                    newView.translationY = (1f - value) * newView.height * 0.75f
                }
                previousView.alpha = 1f - value
                newView.alpha = value
            }
            .addEndListener { _, _, _, _ ->
                switchAnimation = null
                activeContainer.removeView(previousView)
                fragment.onResume()
            }
        switchAnimation = animation
        animation.start()
    }

    open fun onBackPressed(): Boolean {
        val fragment = getCurrentFragment()
        if (fragment?.onBackPressed() == true) return true
        if ((fragmentStack.get(currentSlot)?.size ?: 0) > 1) return destroyCurrent()
        return false
    }
}
