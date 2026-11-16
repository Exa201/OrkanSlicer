package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.view.ProfileDropdownContent
import ru.ytkab0bp.slicebeam.view.ProfileDropdownMenuModel

@RunWith(AndroidJUnit4::class)
class ProfileDropdownLargeTextDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectedProfileTitleKeepsLongProfileReadableAt480DpAnd150PercentText() {
        val selectedTitle = "0.20mm High Quality · Anycubic Kobra 3 v2 0.4 nozzle - Stock Original"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 480
                    screenHeightDp = 2800
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(480.dp)) {
                        ProfileDropdownContent(
                            title = selectedTitle,
                            menu = ProfileDropdownMenuModel(listOf(selectedTitle), 0, false),
                            menuTitle = R.string.SlotPrinterConfigTooltip,
                            expanded = false,
                            onOpen = {},
                            onDismiss = {},
                            onSelect = {},
                            onDelete = {},
                            onClone = {},
                            canManage = false,
                        )
                    }
                }
            }
        }

        val title = compose.onNodeWithTag("profile-dropdown-selected-title", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val heightDp = title.boundsInRoot.height
        assertTrue("At 480 dp and 150% text, the selected profile must wrap over multiple lines", heightDp > 60f)
        assertTrue("The selected profile title should fit in five lines", heightDp <= 180f)
    }
}
