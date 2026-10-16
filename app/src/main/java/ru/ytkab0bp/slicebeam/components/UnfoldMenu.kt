package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.ytkab0bp.slicebeam.fragment.BedFragment

/**
 * Compatibility route adapter for tool contents that have not all been extracted from their
 * AndroidView boundary yet. Presentation, back handling, dimming and responsive sheet geometry
 * are owned by [WorkspaceToolOverlay]; GL and native tool operations remain in their controllers.
 */
abstract class UnfoldMenu {
    @JvmField
    protected var fragment: BedFragment? = null

    private var isVisible = false
    private var isDismissing = false
    private var containerLayout: FrameLayout? = null
    private var rootView: WorkspaceToolOverlay? = null
    private var contentCreated = false
    private var onDismiss: Runnable? = null
    private var workspaceDismiss: (() -> Unit)? = null
    private var workspaceContent: View? = null
    private var workspaceAttachListener: View.OnAttachStateChangeListener? = null
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

    open fun setOnDismiss(onDismiss: Runnable?) {
        this.onDismiss = onDismiss
    }

    protected abstract fun onCreateView(ctx: Context, portrait: Boolean): View

    /**
     * Creates route content for the Compose-owned workspace panel. Migrated tool routes render
     * directly in Compose; remaining Android-backed routes keep a compatibility boundary while
     * sharing the same workspace overlay and back handling.
     */
    internal fun createWorkspaceContent(
        context: Context,
        portrait: Boolean,
        fragment: BedFragment,
    ): WorkspaceUnfoldContent? {
        if (isVisible) return null
        this.fragment = fragment
        isVisible = true
        isDismissing = false
        workspaceDismiss = null
        if (this is ComposeWorkspaceUnfoldMenu) {
            workspaceContent = null
            return WorkspaceUnfoldContent.Compose(portrait)
        }
        val content = try {
            onCreateView(context, portrait)
        } catch (error: Throwable) {
            isVisible = false
            throw error
        }
        workspaceContent = content
        return WorkspaceUnfoldContent.AndroidViewContent(content)
    }

    internal fun setWorkspaceDismiss(dismiss: (() -> Unit)?) {
        workspaceDismiss = dismiss
    }

    @Composable
    internal fun WorkspaceContent(content: WorkspaceUnfoldContent) {
        when (content) {
            is WorkspaceUnfoldContent.Compose -> {
                val composeMenu = this as ComposeWorkspaceUnfoldMenu
                DisposableEffect(this) {
                    workspaceCompositionVisible = true
                    if (!contentCreated) {
                        contentCreated = true
                        onCreate()
                    }
                    onDispose { finishWorkspaceContent() }
                }
                composeMenu.ComposeContent(content.portrait)
            }
            is WorkspaceUnfoldContent.AndroidViewContent -> AndroidView(
                factory = {
                    val view = content.view
                    val listener = workspaceAttachListener ?: object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            if (!contentCreated) {
                                contentCreated = true
                                onCreate()
                            }
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            finishWorkspaceContent()
                        }
                    }.also { workspaceAttachListener = it }
                    view.addOnAttachStateChangeListener(listener)
                    view
                },
                modifier = Modifier.fillMaxSize(),
                update = {},
                onRelease = { view ->
                    finishWorkspaceContent()
                    view.clearAnimation()
                },
            )
        }
    }

    private fun finishWorkspaceContent() {
        val content = workspaceContent
        val attachListener = workspaceAttachListener
        if (content != null && attachListener != null) {
            content.removeOnAttachStateChangeListener(attachListener)
        }
        workspaceAttachListener = null
        val wasCreated = contentCreated
        contentCreated = false
        workspaceCompositionVisible = false
        isVisible = false
        isDismissing = true
        workspaceContent = null
        if (wasCreated) {
            onDestroy()
        } else {
            onDismiss?.run()
            onDismiss = null
        }
        isDismissing = false
        workspaceDismiss = null
    }

    open fun show(from: View, fragment: BedFragment) {
        show(from, fragment, fragment.overlayLayout ?: return)
    }

    open fun show(@Suppress("UNUSED_PARAMETER") from: View, into: FrameLayout) {
        show(from, null, into)
    }

    private fun show(from: View, fragment: BedFragment?, into: FrameLayout) {
        if (isVisible || into.width <= 0 || into.height <= 0) return
        if (fragment != null) this.fragment = fragment
        isVisible = true
        isDismissing = false
        containerLayout = into

        val routeContent = try {
            onCreateView(into.context, into.width < into.height)
        } catch (error: Throwable) {
            isVisible = false
            containerLayout = null
            throw error
        }
        val host = WorkspaceToolOverlay(
            context = into.context,
            routeContent = routeContent,
            panelFraction = { portrait -> requestedPanelFraction(into, portrait) },
            onDismissRequested = { dismiss() },
            onDismissAnimationFinished = { removeHost() },
        ).apply {
            onAttached = {
                if (!contentCreated) {
                    contentCreated = true
                    this@UnfoldMenu.onCreate()
                }
            }
            onDetached = {
                if (contentCreated) {
                    contentCreated = false
                    isVisible = false
                    isDismissing = true
                    this@UnfoldMenu.onDestroy()
                    isDismissing = false
                    rootView = null
                    containerLayout = null
                }
            }
        }
        rootView = host
        into.addView(
            host.view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun requestedPanelFraction(into: FrameLayout, portrait: Boolean): Float {
        val available = (if (portrait) into.height else into.width).coerceAtLeast(0)
        if (available == 0) return 0.6f
        val bounded = UnfoldMenuLayoutPolicy.constrainSide(getRequestedSize(into, portrait), available)
        return (bounded.toFloat() / available).coerceIn(0f, 1f)
    }

    internal fun workspacePanelFraction(into: FrameLayout, portrait: Boolean): Float =
        requestedPanelFraction(into, portrait)

    private fun removeHost() {
        val host = rootView ?: return
        (host.view.parent as? FrameLayout)?.removeView(host.view)
        if (host.view.parent == null && !contentCreated) {
            rootView = null
            containerLayout = null
            isDismissing = false
        }
    }

    protected open fun onShown() = Unit

    open fun isAttached(): Boolean =
        (rootView?.view?.parent != null || workspaceCompositionVisible || workspaceContent?.isAttachedToWindow == true) && !isDismissing

    open fun relayout() {
        rootView?.view?.requestLayout()
        workspaceContent?.requestLayout()
    }

    open fun dismiss() = dismiss(false)

    open fun dismiss(alphaOnly: Boolean) {
        if (!isVisible) return
        isVisible = false
        isDismissing = true
        val dismissWorkspace = workspaceDismiss
        if (dismissWorkspace != null) {
            dismissWorkspace.invoke()
            // Compose removes the panel on the next frame. End the tool session now so back and
            // tool switching cannot leave an active renderer mode during that frame.
            if (this is ComposeWorkspaceUnfoldMenu) finishWorkspaceContent()
        } else {
            rootView?.dismiss(animated = alphaOnly)
        }
    }
}

internal sealed interface WorkspaceUnfoldContent {
    data class Compose(val portrait: Boolean) : WorkspaceUnfoldContent
    data class AndroidViewContent(val view: View) : WorkspaceUnfoldContent
}

/** Tool content that can be composed directly in the workspace while retaining an Android host fallback. */
internal interface ComposeWorkspaceUnfoldMenu {
    @Composable
    fun ComposeContent(portrait: Boolean)
}

/** Bounds requested sheet sizes to the available workspace space. */
internal object UnfoldMenuLayoutPolicy {
    fun constrainSide(requestedSize: Int, availableSize: Int): Int =
        requestedSize.coerceAtLeast(0).coerceAtMost(availableSize.coerceAtLeast(0))
}
