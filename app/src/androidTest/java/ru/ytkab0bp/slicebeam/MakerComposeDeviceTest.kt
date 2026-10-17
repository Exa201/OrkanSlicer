package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.MakerAction
import ru.ytkab0bp.slicebeam.fragment.MakerContent
import ru.ytkab0bp.slicebeam.fragment.MakerSection
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Verifies the Maker section's real expansion and direct-action semantics at narrow, large-text bounds. */
@RunWith(AndroidJUnit4::class)
class MakerComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandableMakerActionHasClearTalkBackActionAndMinimumTouchTarget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expanded = mutableStateOf(emptySet<Int>())
        val sections = listOf(
            MakerSection(
                R.string.MakerConverters,
                R.string.MakerConvertersDescription,
                R.drawable.picture_outline_28,
                listOf(
                    MakerAction(R.string.ImageToStlMenu) {},
                    MakerAction(R.string.LithophaneGeneratorMenu) {},
                ),
            ),
        )
        var launchedAction = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply { screenWidthDp = 320; screenHeightDp = 640 },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        MakerContent(
                            sections = sections,
                            expandedSections = expanded.value,
                            onToggleSection = { index ->
                                expanded.value = if (index in expanded.value) expanded.value - index else expanded.value + index
                            },
                            onAction = { launchedAction = it.titleRes },
                            onBack = {},
                        )
                    }
                }
            }
        }

        val expandDescription = context.getString(
            R.string.MakerSectionToggleDescription,
            context.getString(R.string.MakerConverters),
            context.getString(R.string.MakerExpand),
        )
        compose.onNodeWithContentDescription(expandDescription).assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        val lithophaneTitle = context.getString(R.string.LithophaneGeneratorMenu)
        compose.onNodeWithText(lithophaneTitle).assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(R.string.LithophaneGeneratorMenu, launchedAction) }

        val collapseDescription = context.getString(
            R.string.MakerSectionToggleDescription,
            context.getString(R.string.MakerConverters),
            context.getString(R.string.MakerCollapse),
        )
        compose.onNodeWithContentDescription(collapseDescription).assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(emptySet<Int>(), expanded.value) }
    }

    @Test
    fun singleActionMakerSectionAnnouncesAndRunsTheActionInsteadOfClaimingToExpand() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var launchedAction = 0
        val section = MakerSection(
            R.string.FastenerTitle,
            R.string.FastenerDescription,
            R.drawable.box_heart_outline_28,
            listOf(MakerAction(R.string.FastenerStart) {}),
        )
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply { screenWidthDp = 320; screenHeightDp = 640 },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        MakerContent(
                            sections = listOf(section),
                            expandedSections = emptySet(),
                            onToggleSection = {},
                            onAction = { launchedAction = it.titleRes },
                            onBack = {},
                        )
                    }
                }
            }
        }

        val actionLabel = context.getString(R.string.MakerSectionOpenDescription, context.getString(R.string.FastenerStart))
        compose.onNodeWithContentDescription(actionLabel).assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(R.string.FastenerStart, launchedAction) }
    }
}
