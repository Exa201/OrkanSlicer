package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.PrimitiveModelDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class PrimitiveModelDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun primitiveDialogReturnsSelectedShapeAndDimensions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var result: List<Any>? = null
        compose.setContent {
            BeamComposeTheme {
                PrimitiveModelDialog(onDismiss = {}) { type, a, b, c, name -> result = listOf(type, a, b, c, name) }
            }
        }

        compose.onNodeWithText(context.getString(R.string.PrimitiveCube)).performClick()
        compose.onNodeWithText(context.getString(R.string.PrimitiveCylinder)).performClick()
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("24")
        fields[1].performTextReplacement("12")
        compose.onNodeWithText(context.getString(R.string.PrimitiveCreate)).performClick()

        compose.runOnIdle {
            assertTrue("Create should return the chosen shape", result != null)
            assertEquals(1, result!![0])
            assertEquals(24.0, result!![1])
            assertEquals(0.0, result!![2])
            assertEquals(12.0, result!![3])
            assertEquals(context.getString(R.string.PrimitiveCylinder), result!![4])
        }
    }

    @Test
    fun torusRejectsTubeThatWouldCloseOrSelfIntersectItsHole() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var createdType: Int? = null
        compose.setContent {
            BeamComposeTheme {
                PrimitiveModelDialog(onDismiss = {}) { type, _, _, _, _ -> createdType = type }
            }
        }

        compose.onNodeWithText(context.getString(R.string.PrimitiveCube)).performClick()
        compose.onNodeWithText(context.getString(R.string.PrimitiveTorus)).performClick()
        val create = compose.onNodeWithText(context.getString(R.string.PrimitiveCreate))
        create.assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("4")
        create.performClick()
        compose.runOnIdle { assertEquals(4, createdType) }
    }
}
