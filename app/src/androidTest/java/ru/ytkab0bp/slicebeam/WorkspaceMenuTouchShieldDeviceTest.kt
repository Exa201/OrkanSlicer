package ru.ytkab0bp.slicebeam

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.workspace.consumeUnclaimedPointerEvents
import java.util.concurrent.atomic.AtomicInteger

/** Protects the GL viewport from gestures in transparent workspace-menu gaps. */
@RunWith(AndroidJUnit4::class)
class WorkspaceMenuTouchShieldDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unclaimedMenuGestureDoesNotReachRendererAndMenuButtonsStillWork() {
        val rendererTouchEvents = AtomicInteger()
        val menuClicks = AtomicInteger()

        compose.setContent {
            Box(Modifier.size(240.dp)) {
                AndroidView(
                    factory = { context ->
                        View(context).apply {
                            setOnTouchListener { _, event ->
                                if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                                    event.actionMasked == MotionEvent.ACTION_MOVE) {
                                    rendererTouchEvents.incrementAndGet()
                                }
                                true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .consumeUnclaimedPointerEvents()
                        .testTag("workspace-menu-touch-shield"),
                ) {
                    Button(
                        onClick = { menuClicks.incrementAndGet() },
                        modifier = Modifier.align(Alignment.TopStart),
                    ) { Text("Menu action") }
                }
            }
        }

        compose.onNodeWithTag("workspace-menu-touch-shield").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(210f, 210f))
            moveTo(androidx.compose.ui.geometry.Offset(220f, 220f), 40)
            up()
        }
        compose.runOnIdle {
            assertEquals("an unclaimed gesture in menu padding must not reach the renderer", 0,
                rendererTouchEvents.get())
        }

        compose.onNodeWithText("Menu action").performClick()
        compose.runOnIdle {
            assertEquals("interactive menu children must keep receiving taps", 1, menuClicks.get())
            assertEquals("menu-button input must stay isolated from the renderer", 0,
                rendererTouchEvents.get())
        }
    }
}
