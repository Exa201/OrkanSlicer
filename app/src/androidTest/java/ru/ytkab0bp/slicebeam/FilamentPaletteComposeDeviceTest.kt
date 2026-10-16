package ru.ytkab0bp.slicebeam

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.PlateTabs
import ru.ytkab0bp.slicebeam.ui.workspace.PlateThumbnail
import ru.ytkab0bp.slicebeam.components.AcePhysicalSlotsBar
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.view.FilamentPaletteContent
import ru.ytkab0bp.slicebeam.view.PaletteSlotSnapshot
import ru.ytkab0bp.slicebeam.components.bed_menu.BedComposeMenuAction
import ru.ytkab0bp.slicebeam.components.bed_menu.BedMenuActionsContent

/** Exercises the real virtualized palette row with both supported and synthetic UI-only sizes. */
@RunWith(AndroidJUnit4::class)
class FilamentPaletteComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun longAceGateRowHasAccessibleScrollControlAndReachesLastGate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        AcePhysicalSlotsBar(
                            gates = List(24) { index ->
                                MmuFilamentSync.Companion.PhysicalGate(
                                    index = index,
                                    printerTool = index,
                                    occupied = true,
                                    filament = FilamentSlot(0xFF000000.toInt() or (index * 0x010101), "Synthetic PLA $index"),
                                )
                            },
                        )
                    }
                }
            }
        }

        val scrollForward = compose.onNodeWithContentDescription(
            context.getString(R.string.AcePhysicalSlotsScrollForward),
        ).assertIsDisplayed()
        scrollForward.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Synthetic PLA 23").assertIsDisplayed()
    }

    @Test
    fun platePickerCancelHasAccessibleTouchTargetAndDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val open = mutableStateOf(false)
        var openCalls = 0
        var dismissCalls = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    PlateTabs(
                        openState = open,
                        plateCount = 3,
                        selectedIndex = 0,
                        objectCounts = listOf(0, 1, 2),
                        thumbnails = List(3) { PlateThumbnail(emptyList()) },
                        onOpen = { openCalls++ },
                        onDismiss = { dismissCalls++ },
                        onSelect = {},
                        onClose = {},
                        onAdd = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(context.getString(R.string.PlateNumber, "1")).performClick()
        val cancelLabel = context.getString(android.R.string.cancel)
        compose.onNodeWithText(cancelLabel).assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(0, compose.onAllNodesWithText(cancelLabel).fetchSemanticsNodes().size)
        compose.runOnIdle {
            assertFalse(open.value)
            assertEquals(1, openCalls)
            assertEquals(1, dismissCalls)
        }
    }

    @Test
    fun paletteRowsFromOneThroughTwentyFourEntriesScrollAndSelectTheLastItem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val count = mutableIntStateOf(1)
        val widthDp = mutableIntStateOf(320)
        val fontScale = mutableFloatStateOf(1f)
        var selectedIndex = -1

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.floatValue)) {
                BeamComposeTheme {
                    val slots = List(count.intValue) { index ->
                        PaletteSlotSnapshot(
                            color = 0xFF000000.toInt() or ((index * 0x010307) and 0x00FFFFFF),
                            type = "Synthetic PLA $index with a long material label",
                        )
                    }
                    Box(Modifier.width(widthDp.intValue.dp)) {
                        FilamentPaletteContent(
                            slots = slots,
                            selectedGates = null,
                            onSlotClick = { selectedIndex = it },
                            onAddClick = {},
                        )
                    }
                }
            }
        }

        listOf(320 to 1f, 360 to 1.3f, 400 to 1.5f).forEach { (screenWidth, textScale) ->
            compose.runOnUiThread {
                widthDp.intValue = screenWidth
                fontScale.floatValue = textScale
            }
            listOf(1, 4, 8, 16, 24).forEach { slotCount ->
                compose.runOnUiThread { count.intValue = slotCount }
                compose.waitForIdle()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(slotCount - 1)
                val lastIndex = slotCount - 1
                val description = context.getString(
                    R.string.FilamentPaletteSlotDescription,
                    lastIndex,
                    "Synthetic PLA $lastIndex with a long material label",
                )
                val slot = compose.onNodeWithContentDescription(description).assertIsDisplayed()
                slot.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                slot.performClick()
                compose.runOnIdle { assertEquals(lastIndex, selectedIndex) }
            }
        }
    }

    @Test
    fun twoRowToolMenuKeepsLastActionReachableAtNarrowWidthAndLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actionTitles = listOf(
            R.string.MenuCameraIsometric, R.string.MenuCameraTop, R.string.MenuCameraBottom,
            R.string.MenuCameraFront, R.string.MenuCameraBack, R.string.MenuCameraLeft,
            R.string.MenuCameraRight, R.string.MenuCameraOrtho, R.string.MenuCameraControlMode,
            R.string.MenuModel,
        )
        var selectedTitle = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp).height(150.dp)) {
                        BedMenuActionsContent(
                            actions = actionTitles.map { title ->
                                BedComposeMenuAction(
                                    titleRes = title,
                                    iconRes = R.drawable.camera_mode_0_28,
                                    onClick = { selectedTitle = title },
                                )
                            },
                            portrait = true,
                            multiRow = true,
                        )
                    }
                }
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(actionTitles.lastIndex)
        val lastTitle = context.getString(actionTitles.last())
        val lastAction = compose.onNodeWithContentDescription(lastTitle).assertIsDisplayed()
        lastAction.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        lastAction.performClick()
        compose.runOnIdle { assertEquals(actionTitles.last(), selectedTitle) }
    }
}
