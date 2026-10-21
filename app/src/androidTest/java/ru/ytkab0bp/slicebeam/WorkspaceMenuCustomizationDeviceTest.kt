package ru.ytkab0bp.slicebeam

import android.view.View
import android.R as AndroidR
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.BedComposeMenuAction
import ru.ytkab0bp.slicebeam.components.bed_menu.CustomizableBedMenuActionsContent
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraMenuContent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class WorkspaceMenuCustomizationDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun menuActionsCanBeHiddenReorderedAndPersisted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val menuKey = "device_test"
        context.getSharedPreferences("workspace_menu_customization", 0).edit().clear().commit()
        val topKey = context.resources.getResourceEntryName(R.string.MenuCameraTop)
        val bottomKey = context.resources.getResourceEntryName(R.string.MenuCameraBottom)
        val actions = listOf(
            BedComposeMenuAction(R.string.MenuCameraTop, R.drawable.camera_outline_28, ::noOp),
            BedComposeMenuAction(R.string.MenuCameraBottom, R.drawable.camera_outline_28, ::noOp),
        )

        compose.setContent {
            BeamComposeTheme {
                var generation by remember { mutableIntStateOf(0) }
                Column {
                    androidx.compose.material3.TextButton(
                        onClick = { generation++ },
                        modifier = Modifier.testTag("force-remount-$menuKey"),
                    ) { androidx.compose.material3.Text("Remount") }
                    Box(Modifier.width(360.dp)) {
                        key(generation) {
                            CustomizableBedMenuActionsContent(menuKey, actions, portrait = true)
                        }
                    }
                }
            }
        }

        compose.onNodeWithText(context.getString(R.string.MenuCameraTop)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.MenuCameraBottom)).assertIsDisplayed()
        dragAction(
            "workspace-menu-action-${R.string.MenuCameraTop}",
            "workspace-menu-action-${R.string.MenuCameraBottom}",
        )
        compose.onNodeWithTag("menu-customize-$menuKey").performClick()
        compose.onNodeWithTag("menu-customize-visible-$menuKey-$bottomKey").performClick()
        compose.onNodeWithTag("menu-customize-save-$menuKey").performClick()

        compose.onNodeWithText(context.getString(R.string.MenuCameraBottom)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.MenuCameraTop)).assertIsDisplayed()
        val prefs = context.getSharedPreferences("workspace_menu_customization", 0)
        assertTrue(prefs.getString("order_$menuKey", "").orEmpty().startsWith("$bottomKey|"))
        assertTrue(bottomKey in prefs.getStringSet("hidden_$menuKey", emptySet()).orEmpty())

        // Discard remembered composable state and reconstruct from persisted preferences.
        compose.onNodeWithTag("force-remount-$menuKey").performClick()
        compose.onNodeWithText(context.getString(R.string.MenuCameraBottom)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.MenuCameraTop)).assertIsDisplayed()
        compose.onNodeWithTag("menu-customize-$menuKey").performClick()
        compose.onNodeWithTag("menu-customize-visible-$menuKey-$bottomKey").assertIsDisplayed()
    }

    @Test fun cameraMenuUsesCustomizationAndPersistsHiddenAndReorderedActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val menuKey = "view"
        val prefs = context.getSharedPreferences("workspace_menu_customization", 0)
        val previousOrder = prefs.getString("order_$menuKey", null)
        val previousHidden = prefs.getStringSet("hidden_$menuKey", emptySet())?.toSet()
        prefs.edit().remove("order_$menuKey").remove("hidden_$menuKey").commit()
        val cameraKey = context.resources.getResourceEntryName(R.string.MenuCameraControlMode)
        val orthoKey = context.resources.getResourceEntryName(R.string.MenuCameraOrtho)

        try {
            compose.setContent {
                BeamComposeTheme {
                    Box(Modifier.width(360.dp)) {
                        CameraMenuContent(
                            portrait = true,
                            initialOrthoEnabled = true,
                            onControlModeSelected = {},
                            onOrthoChanged = {},
                        )
                    }
                }
            }

            compose.onNodeWithTag("menu-customize-view").assertIsDisplayed().performClick()
            compose.onNodeWithText(context.getString(AndroidR.string.cancel)).performClick()
            dragAction(
                "workspace-menu-action-${R.string.MenuCameraOrtho}",
                "workspace-menu-action-${R.string.MenuCameraControlMode}",
            )
            compose.onNodeWithTag("menu-customize-view").performClick()
            compose.onNodeWithTag("menu-customize-visible-view-$orthoKey").performClick()
            compose.onNodeWithTag("menu-customize-save-view").performClick()

            compose.onNodeWithText(context.getString(R.string.MenuCameraOrtho)).assertDoesNotExist()
            compose.onNodeWithText(context.getString(R.string.MenuCameraControlMode)).assertIsDisplayed()
            assertTrue(prefs.getString("order_$menuKey", "").orEmpty().startsWith("$cameraKey|"))
            assertTrue(orthoKey in prefs.getStringSet("hidden_$menuKey", emptySet()).orEmpty())
        } finally {
            prefs.edit().also { editor ->
                if (previousOrder == null) editor.remove("order_$menuKey") else editor.putString("order_$menuKey", previousOrder)
                if (previousHidden == null) editor.remove("hidden_$menuKey") else editor.putStringSet("hidden_$menuKey", previousHidden)
            }.commit()
        }
    }

    @Test fun cameraMenuCustomizationIsReachableAtCompactWidths() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val width = mutableIntStateOf(320)
        compose.setContent {
            BeamComposeTheme {
                Box(Modifier.width(width.intValue.dp)) {
                    CameraMenuContent(
                        portrait = true,
                        initialOrthoEnabled = true,
                        onControlModeSelected = {},
                        onOrthoChanged = {},
                    )
                }
            }
        }

        listOf(320, 360, 400).forEach { widthDp ->
            compose.runOnIdle { width.intValue = widthDp }
            compose.waitForIdle()
            compose.onNodeWithTag("menu-customize-view").assertIsDisplayed()
            compose.onNodeWithContentDescription(context.getString(R.string.MenuCustomize)).assertIsDisplayed()
            compose.onNodeWithTag("menu-customize-view").performClick()
            compose.onNodeWithTag("menu-customize-list-view").assertIsDisplayed()
            compose.onNodeWithText(context.getString(AndroidR.string.cancel)).performClick()
        }
    }

    @Test fun cameraMenuCustomizationRemainsUsableWithLargeTextAndLandscapeLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fontScale = mutableFloatStateOf(1f)
        val portrait = mutableStateOf(true)
        val widthDp = mutableIntStateOf(360)
        val changedOrtho = mutableStateOf<Boolean?>(null)
        compose.setContent {
            val baseDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(baseDensity, fontScale.floatValue)) {
                BeamComposeTheme {
                    Box(Modifier.width(widthDp.intValue.dp)) {
                        key(fontScale.floatValue, portrait.value, widthDp.intValue) {
                            CameraMenuContent(
                                portrait = portrait.value,
                                initialOrthoEnabled = true,
                                onControlModeSelected = {},
                                onOrthoChanged = { changedOrtho.value = it },
                            )
                        }
                    }
                }
            }
        }

        listOf(320, 360, 400).forEach { width ->
            listOf(1f, 1.3f, 1.5f).forEach { scale ->
                listOf(true, false).forEach { isPortrait ->
                    compose.runOnIdle {
                        widthDp.intValue = width
                        fontScale.floatValue = scale
                        portrait.value = isPortrait
                    }
                    compose.waitForIdle()
                    compose.onNodeWithTag("menu-customize-view").assertIsDisplayed()
                    if (isPortrait) {
                        compose.onNodeWithContentDescription(context.getString(R.string.MenuCustomize)).assertIsDisplayed()
                    } else {
                        compose.onNodeWithText(context.getString(R.string.MenuCustomize)).assertIsDisplayed()
                    }
                    compose.onNodeWithTag("menu-customize-view").performClick()
                    compose.onNodeWithTag("menu-customize-list-view").assertIsDisplayed()
                    compose.onNodeWithTag("menu-customize-visible-view-${context.resources.getResourceEntryName(R.string.MenuCameraControlMode)}")
                        .assertIsDisplayed()
                    compose.onNodeWithText(context.getString(AndroidR.string.cancel)).performClick()
                    compose.onNodeWithTag("workspace-menu-action-${R.string.MenuCameraOrtho}").assertIsDisplayed()
                    compose.onNodeWithContentDescription(context.getString(R.string.MenuCameraOrtho)).assertIsDisplayed()
                    compose.onNodeWithTag("workspace-menu-action-${R.string.MenuCameraOrtho}").performClick()
                    compose.runOnIdle { assertEquals(false, changedOrtho.value) }
                }
            }
        }
    }

    private fun noOp(view: View) = Unit

    private fun dragAction(sourceTag: String, targetTag: String) {
        val source = compose.onNodeWithTag(sourceTag)
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val targetCenter = compose.onNodeWithTag(targetTag).fetchSemanticsNode().boundsInRoot.center
        source.performTouchInput {
            down(center)
            advanceEventTime(600)
            moveTo(targetCenter - sourceBounds.topLeft, durationMillis = 500)
            up()
        }
    }
}
