package ru.ytkab0bp.slicebeam.components

import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelSize

/**
 * Lifecycle adapter for a Compose-owned workspace tool route. Native/GL operations remain in
 * their existing controllers; this class only binds the route to the exclusive workspace panel.
 */
abstract class WorkspaceComposeToolRoute {
    @JvmField
    protected var fragment: BedFragment? = null

    private var isVisible = false
    private var isDismissing = false
    private var contentCreated = false
    private var onDismiss: Runnable? = null
    private var workspaceDismiss: (() -> Unit)? = null
    private var workspaceCompositionVisible = false

    internal open fun workspacePanelSize(): WorkspacePanelSize = WorkspacePanelSize()

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
    ): WorkspaceToolContent? {
        if (isVisible) return null
        this.fragment = fragment
        isVisible = true
        isDismissing = false
        workspaceDismiss = null
        return WorkspaceToolContent(portrait)
    }

    internal fun setWorkspaceDismiss(dismiss: (() -> Unit)?) {
        workspaceDismiss = dismiss
    }

    @Composable
    internal fun WorkspaceContent(content: WorkspaceToolContent) {
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

    open fun isAttached(): Boolean = workspaceCompositionVisible && !isDismissing

    open fun dismiss() {
        if (!isVisible) return
        isVisible = false
        isDismissing = true
        workspaceDismiss?.invoke()
        finishWorkspaceContent()
    }
}

internal data class WorkspaceToolContent(val portrait: Boolean)

/** Bounds requested sheet sizes to the available workspace space. */
