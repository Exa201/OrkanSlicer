package ru.ytkab0bp.slicebeam

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.BedComposeMenuAction
import ru.ytkab0bp.slicebeam.components.bed_menu.CustomizableBedMenuActionsContent
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
        compose.onNodeWithTag("menu-customize-$menuKey").performClick()
        compose.onNodeWithTag("menu-customize-down-$menuKey-$topKey").performClick()
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
        compose.onNodeWithTag("menu-customize-down-$menuKey-$bottomKey").assertIsDisplayed()
    }

    private fun noOp(view: View) = Unit
}
