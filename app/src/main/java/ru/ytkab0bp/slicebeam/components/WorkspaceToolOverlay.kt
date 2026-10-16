package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.delay
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Compose-owned sheet and dismissal layer for the existing tool route content. */
internal class WorkspaceToolOverlay(
    context: Context,
    private val routeContent: View,
    private val panelFraction: (portrait: Boolean) -> Float,
    private val onDismissRequested: () -> Unit,
    private val onDismissAnimationFinished: () -> Unit,
) {
    private var visible by mutableStateOf(true)
    private var exitDurationMs by mutableIntStateOf(DEFAULT_EXIT_DURATION_MS)
    var onAttached: (() -> Unit)? = null
    var onDetached: (() -> Unit)? = null
    private var wasAttached = false
    val view = ComposeView(context)

    init {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            BeamComposeTheme {
                WorkspaceToolOverlayContent(
                    visible = visible,
                    exitDurationMs = exitDurationMs,
                    panelFraction = panelFraction,
                    onDismiss = onDismissRequested,
                    onDismissAnimationFinished = onDismissAnimationFinished,
                ) {
                    AndroidView(
                        factory = { routeContent },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                if (!wasAttached) {
                    wasAttached = true
                    onAttached?.invoke()
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                if (wasAttached) {
                    wasAttached = false
                    onDetached?.invoke()
                }
            }
        })
    }

    fun dismiss(animated: Boolean) {
        if (!visible) return
        exitDurationMs = if (animated) 150 else DEFAULT_EXIT_DURATION_MS
        visible = false
    }

    private companion object {
        const val DEFAULT_EXIT_DURATION_MS = 180
    }
}

@Composable
internal fun WorkspaceToolOverlayContent(
    visible: Boolean,
    exitDurationMs: Int,
    panelFraction: (portrait: Boolean) -> Float,
    onDismiss: () -> Unit,
    onDismissAnimationFinished: () -> Unit,
    routeContent: @Composable () -> Unit,
) {
    LaunchedEffect(visible, exitDurationMs) {
        if (!visible) {
            delay(exitDurationMs.toLong())
            onDismissAnimationFinished()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        val sheetSize: Dp = (if (portrait) maxHeight else maxWidth) * panelFraction(portrait).coerceIn(0f, 1f)
        val dismissLabel = stringResource(android.R.string.cancel)
        val shape = if (portrait) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        } else {
            RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .semantics { contentDescription = dismissLabel }
                .clickable(role = Role.Button, onClick = onDismiss),
        )
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(if (portrait) Alignment.BottomCenter else Alignment.CenterStart),
            enter = if (portrait) {
                fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 4 }
            } else {
                fadeIn(tween(180)) + slideInHorizontally(tween(180)) { -it / 4 }
            },
            exit = if (portrait) {
                fadeOut(tween(exitDurationMs)) + slideOutVertically(tween(exitDurationMs)) { it / 4 }
            } else {
                fadeOut(tween(exitDurationMs)) + slideOutHorizontally(tween(exitDurationMs)) { -it / 4 }
            },
        ) {
            Surface(
                modifier = if (portrait) Modifier.fillMaxWidth().height(sheetSize) else Modifier
                    .fillMaxHeight()
                    .width(sheetSize),
                color = MaterialTheme.colorScheme.surface,
                shape = shape,
                shadowElevation = 8.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    routeContent()
                }
            }
        }
    }
}
