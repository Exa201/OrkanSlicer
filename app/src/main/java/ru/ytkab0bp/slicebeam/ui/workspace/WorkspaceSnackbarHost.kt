package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.ytkab0bp.slicebeam.ui.components.SnackbarCard
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceToolPanelPolicy
import ru.ytkab0bp.slicebeam.view.SnackbarSwipePolicy
import kotlin.math.roundToInt

/** Compose overlay for workspace notifications; it does not reserve or resize the renderer. */
@Composable
internal fun WorkspaceSnackbarHost(
    controller: WorkspaceSnackbarController,
    portrait: Boolean,
    toolPanelExpanded: Boolean,
    toolPanelHeightDp: Int,
    navigationHeightDp: Int,
    startInsetDp: Int,
    modifier: Modifier = Modifier,
) {
    val bottomInset = if (portrait) {
        WorkspaceToolPanelPolicy.reservedHeightDp(true, toolPanelExpanded, toolPanelHeightDp) +
            navigationHeightDp
    } else {
        0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = startInsetDp.dp, end = 8.dp, bottom = bottomInset.dp + 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            controller.entries.forEach { entry ->
                key(entry.id) {
                    val snackbar = entry.snackbar
                    val offset = remember(entry.id) { Animatable(0f) }
                    val coroutineScope = rememberCoroutineScope()
                    val anchorView = LocalView.current
                    val density = LocalDensity.current

                    LaunchedEffect(entry.id, entry.visible) {
                        if (entry.visible && snackbar.lifetime > 0) {
                            delay(snackbar.lifetime.toLong())
                            controller.dismiss(entry.id)
                        } else if (!entry.visible) {
                            // Keep the entry through the exit animation, then release its state.
                            delay(EXIT_ANIMATION_MS)
                            controller.remove(entry.id)
                        }
                    }

                    AnimatedVisibility(
                        visible = entry.visible,
                        enter = fadeIn(tween(EXIT_ANIMATION_MS.toInt())) +
                            slideInVertically(tween(EXIT_ANIMATION_MS.toInt())) { it / 2 },
                        exit = fadeOut(tween(EXIT_ANIMATION_MS.toInt())) +
                            slideOutVertically(tween(EXIT_ANIMATION_MS.toInt())) { it / 2 },
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(offset.value.roundToInt(), 0) },
                        ) {
                            val widthPx = with(density) { maxWidth.roundToPx() }
                            SnackbarCard(
                                type = snackbar.type,
                                title = snackbar.title,
                                buttonTitle = snackbar.buttonTitle,
                                onButtonClick = { snackbar.buttonClick?.onClick(anchorView) },
                                onHorizontalDrag = { distance ->
                                    coroutineScope.launch { offset.snapTo(offset.value + distance) }
                                },
                                onDragEnd = { velocity ->
                                    val release = SnackbarSwipePolicy.release(
                                        lifetimeMs = snackbar.lifetime,
                                        velocityX = velocity,
                                        viewWidth = widthPx,
                                        marginPx = with(density) { 8.dp.roundToPx() },
                                    )
                                    coroutineScope.launch {
                                        offset.animateTo(release.targetX, spring())
                                        if (release.dismiss) {
                                            controller.dismiss(entry.id)
                                        } else {
                                            offset.animateTo(0f, spring())
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch { offset.animateTo(0f, spring()) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val EXIT_ANIMATION_MS = 240L
