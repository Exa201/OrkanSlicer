package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.text.TextLayoutResult
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.view.ProfileDropdownContent
import ru.ytkab0bp.slicebeam.view.ProfileDropdownMenuPolicy

@RunWith(AndroidJUnit4::class)
class ProfileDropdownLayoutDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longProfileNamesWrapFullyAtNarrowWidthAndLargeText() {
        val longName = "Anycubic Kobra 3 V2 · 0,4 mm Düse · High Quality für Mehrfarben-Druck"
        val titleLayout = AtomicReference<TextLayoutResult?>()
        val itemLayout = AtomicReference<TextLayoutResult?>()
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        ProfileDropdownContent(
                            title = longName,
                            menu = ProfileDropdownMenuPolicy.create(listOf(longName), 0),
                            menuTitle = R.string.WorkspaceProfilePrinter,
                            expanded = true,
                            onOpen = {},
                            onDismiss = {},
                            onSelect = {},
                            onDelete = {},
                            onClone = {},
                            onTitleTextLayout = { titleLayout.set(it) },
                            onMenuItemTextLayout = { _, result -> itemLayout.set(result) },
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("profile-dropdown-title", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("profile-dropdown-item-0", useUnmergedTree = true).assertIsDisplayed()
        assertTrue("The selected profile title should wrap", (titleLayout.get()?.lineCount ?: 0) > 1)
        assertTrue("The menu profile name should wrap", (itemLayout.get()?.lineCount ?: 0) > 1)
        assertFalse("The selected profile title must not clip", titleLayout.get()?.didOverflowHeight ?: true)
        assertFalse("The menu profile name must not clip", itemLayout.get()?.didOverflowHeight ?: true)
    }
}
