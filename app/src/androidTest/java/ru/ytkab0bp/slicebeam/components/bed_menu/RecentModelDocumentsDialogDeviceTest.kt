package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.utils.RecentModelDocument
import ru.ytkab0bp.slicebeam.R

@RunWith(AndroidJUnit4::class)
class RecentModelDocumentsDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun recentDocumentIsVisibleAndCanBeReopened() {
        val document = RecentModelDocument("content://provider/model.3mf", "assembly.3mf")
        var opened: RecentModelDocument? = null
        compose.setContent {
            MaterialTheme {
                RecentModelDocumentsDialog(
                    documents = listOf(document),
                    onOpen = { opened = it },
                    onDismiss = {},
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.RecentModelsTitle)).assertIsDisplayed()
        compose.onNodeWithText("assembly.3mf").assertIsDisplayed()
        compose.onNodeWithTag("recent-model-document-0").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(document, opened) }
    }
}
