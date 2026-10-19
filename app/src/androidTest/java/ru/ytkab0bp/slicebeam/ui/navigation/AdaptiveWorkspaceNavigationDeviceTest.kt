package ru.ytkab0bp.slicebeam.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class AdaptiveWorkspaceNavigationDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactNavigationKeepsAllDestinationsReachableAndReportsSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val selected = mutableIntStateOf(WorkspaceDestination.PROJECT.ordinal)
        val items = WorkspaceDestination.values().map { destination ->
            WorkspaceNavigationItem(
                slot = destination.ordinal,
                label = context.getString(destination.navigationTitleRes),
                icon = destination.iconRes,
                accessibilityLabel = context.getString(destination.navigationTitleRes),
            )
        }

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        AdaptiveWorkspaceNavigation(
                            items = items,
                            selectedSlot = selected.intValue,
                            isBottomNavigation = true,
                            onSelected = { selected.intValue = it },
                        )
                    }
                }
            }
        }

        val editLabel = context.getString(WorkspaceDestination.EDIT.navigationTitleRes)
        WorkspaceDestination.values().forEach { destination ->
            compose.onNodeWithContentDescription(context.getString(destination.navigationTitleRes))
                .assertIsDisplayed()
        }
        compose.onNodeWithContentDescription(editLabel).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(WorkspaceDestination.EDIT.ordinal, selected.intValue) }
    }

    @Test
    fun largeTextUsesTwoRowsWithoutLosingDestinationAccessibilityLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val items = WorkspaceDestination.values().map { destination ->
            WorkspaceNavigationItem(
                slot = destination.ordinal,
                label = context.getString(destination.navigationTitleRes),
                icon = destination.iconRes,
                accessibilityLabel = context.getString(destination.navigationTitleRes),
                compactLabel = context.getString(
                    if (destination == WorkspaceDestination.TOOLS) R.string.MenuOptionsCompact
                    else destination.titleRes,
                ),
            )
        }

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        AdaptiveWorkspaceNavigation(
                            items = items,
                            selectedSlot = WorkspaceDestination.PROJECT.ordinal,
                            isBottomNavigation = true,
                            onSelected = {},
                        )
                    }
                }
            }
        }

        WorkspaceDestination.values().forEach { destination ->
            compose.onNodeWithContentDescription(context.getString(destination.navigationTitleRes))
                .assertIsDisplayed()
        }
        compose.onNodeWithText(context.getString(R.string.MenuOptionsCompact)).assertIsDisplayed()
    }

    @Test
    fun shortNavigationRailShowsScrollHintAndCanReachLastDestination() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val items = WorkspaceDestination.values().map { destination ->
            WorkspaceNavigationItem(
                slot = destination.ordinal,
                label = context.getString(destination.navigationTitleRes),
                icon = destination.iconRes,
                accessibilityLabel = context.getString(destination.navigationTitleRes),
            )
        }

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                BeamComposeTheme {
                    Box(Modifier.width(100.dp).height(180.dp)) {
                        AdaptiveWorkspaceNavigation(
                            items = items,
                            selectedSlot = WorkspaceDestination.PROJECT.ordinal,
                            isBottomNavigation = false,
                            onSelected = {},
                        )
                    }
                }
            }
        }

        val lastDestination = WorkspaceDestination.SLICE
        val lastLabel = context.getString(lastDestination.navigationTitleRes)
        compose.onNodeWithContentDescription(context.getString(R.string.NavigationScrollMoreBelow))
            .assertIsDisplayed()

        compose.onNodeWithContentDescription(lastLabel).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.NavigationScrollMoreAbove))
            .assertIsDisplayed()
    }
}
