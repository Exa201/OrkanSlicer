package ru.ytkab0bp.slicebeam.components

import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import ru.ytkab0bp.slicebeam.fragment.BedFragment

/**
 * Lifecycle adapter for a Compose-owned workspace tool route. Native/GL operations remain in
 * their existing controllers; this class only binds the route to the exclusive workspace panel.
 */
abstract class UnfoldMenu {
    @JvmField
    protected var fragment: BedFragment? = null

    private var isVisible = false
    private var isDismissing = false
    private var contentCreated = false
    private var onDismiss: Runnable? = null
    private var workspaceDismiss: (() -> Unit)? = null
    private var workspaceCompositionVisible = false

    open fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
        ((if (portrait) into.height else into.width) * 0.6f).toInt()

    @CallSuper
    protected open fun onCreate() = Unit

    @CallSuper
    protected open fun onDestroy() {
        onDismiss?.run()
        onDismiss = null
    }

    @Composable
    abstract fun ComposeContent(portrait: Boolean)

    fun setOnDismiss(onDismiss: Runnable?) {
        this.onDismiss = onDismiss
    }

    internal fun createWorkspaceContent(
        portrait: Boolean,
        fragment: BedFragment,
    ): WorkspaceUnfoldContent? {
        if (isVisible) return null
        this.fragment = fragment
        isVisible = true
        isDismissing = false
        workspaceDismiss = null
        return WorkspaceUnfoldContent(portrait)
    }

    internal fun setWorkspaceDismiss(dismiss: (() -> Unit)?) {
        workspaceDismiss = dismiss
    }

    @Composable
    internal fun WorkspaceContent(content: WorkspaceUnfoldContent) {
        DisposableEffect(this, content) {
            workspaceCompositionVisible = true
            if (!contentCreated) {
                contentCreated = true
                onCreate()
            }
            onDispose { finishWorkspaceContent() }
        }
        ComposeContent(content.portrait)
    }

    private fun finishWorkspaceContent() {
        if (!contentCreated && !workspaceCompositionVisible && onDismiss == null) return
        val wasCreated = contentCreated
        contentCreated = false
        workspaceCompositionVisible = false
        isVisible = false
        isDismissing = true
        if (wasCreated) {
            onDestroy()
        } else {
            onDismiss?.run()
            onDismiss = null
        }
        isDismissing = false
        workspaceDismiss = null
    }

    internal fun workspacePanelFraction(into: FrameLayout, portrait: Boolean): Float {
        val available = (if (portrait) into.height else into.width).coerceAtLeast(0)
        if (available == 0) return 0.6f
        val bounded = UnfoldMenuLayoutPolicy.constrainSide(getRequestedSize(into, portrait), available)
        return (bounded.toFloat() / available).coerceIn(0f, 1f)
    }

    open fun isAttached(): Boolean = workspaceCompositionVisible && !isDismissing

    open fun dismiss() {
        if (!isVisible) return
        isVisible = false
        isDismissing = true
        workspaceDismiss?.invoke()
        finishWorkspaceContent()
    }
}

internal data class WorkspaceUnfoldContent(val portrait: Boolean)

/** Bounds requested sheet sizes to the available workspace space. */
internal object UnfoldMenuLayoutPolicy {
    fun constrainSide(requestedSize: Int, availableSize: Int): Int =
        requestedSize.coerceAtLeast(0).coerceAtMost(availableSize.coerceAtLeast(0))
}
