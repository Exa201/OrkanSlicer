package ru.ytkab0bp.slicebeam

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.workspace.PrinterWebWorkspacePanel

@RunWith(AndroidJUnit4::class)
class PrinterWebWorkspacePanelDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun technicalPrinterViewIsAttachedOnlyWhileComposeWorkspacePanelIsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val panelView = FrameLayout(context)
        val visible = mutableStateOf(true)

        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                if (visible.value) PrinterWebWorkspacePanel(panelView)
            }
        }

        compose.runOnIdle { assertNotNull("The active Compose panel hosts its Android view", panelView.parent) }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { assertNull("Closing the panel detaches the Android view", panelView.parent) }
    }
}
