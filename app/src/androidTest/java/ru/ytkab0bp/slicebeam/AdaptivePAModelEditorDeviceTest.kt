package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileStringVectorOption
import ru.ytkab0bp.slicebeam.config.ProfileStringVectorCodec
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@RunWith(AndroidJUnit4::class)
class AdaptivePAModelEditorDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun malformedEditedAdaptiveModelShowsVariantAndLineAndDisablesSave() {
        installEditor("0.04,10,3000;0,0,0")
        compose.onNodeWithText("Adaptive PA").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("0.04, 10")

        val message = context.getString(
            R.string.AdaptivePAModelValidationLine,
            1,
            1,
            context.getString(R.string.AdaptivePAModelValueCount),
        )
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ProfileEnumVectorSave)).assertIsNotEnabled()
    }

    @Test
    fun untouchedPlaceholderVariantDoesNotBlockSavingEditedWhitespaceModel() {
        val saved = AtomicReference<String?>()
        installEditor("0,0,0;0,0,0", saved)
        compose.onNodeWithText("Adaptive PA").performClick()
        compose.onAllNodes(hasSetTextAction())[0]
            .performTextReplacement(" 0.027 , 8.1416 , 2000.0 \n 0.025, 12, 5000 ")

        compose.onNodeWithText(context.getString(R.string.ProfileEnumVectorSave))
            .assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(
            listOf(" 0.027 , 8.1416 , 2000.0 \n 0.025, 12, 5000 ", "0,0,0"),
            ProfileStringVectorCodec.parse(saved.get().orEmpty()),
        )
    }

    private fun installEditor(initialValue: String, saved: AtomicReference<String?> = AtomicReference()) {
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileStringVectorOption.Content(
                    title = "Adaptive PA",
                    initialValue = initialValue,
                    description = "PA, flow, acceleration",
                    multiline = true,
                    preferredLines = 6,
                    suggestionLabels = null,
                    suggestionValues = null,
                    onChanged = Consumer(saved::set),
                    onLongClick = Runnable { },
                    optionKey = "adaptive_pressure_advance_model",
                )
            }
        }
        compose.onNodeWithText("Adaptive PA").assertIsDisplayed()
    }
}
