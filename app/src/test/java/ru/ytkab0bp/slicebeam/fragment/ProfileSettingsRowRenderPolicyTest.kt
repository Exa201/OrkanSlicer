package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ProfileOpenOptionResolver
import ru.ytkab0bp.slicebeam.recycler.PreferenceItem
import ru.ytkab0bp.slicebeam.recycler.SpaceItem
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import java.util.function.Consumer
import java.util.function.Supplier

class ProfileSettingsRowRenderPolicyTest {
    @Test
    fun builtInRowsAndProfileOptionsUseCompose() {
        assertTrue(ProfileSettingsRowRenderPolicy.usesDirectCompose(ProfileSubHeader("Quality"), false))
        assertTrue(
            ProfileSettingsRowRenderPolicy.usesDirectCompose(
                ProfileSubHint(PreferenceItem.ValueProvider { "Helpful note" }),
                false,
            ),
        )
        assertTrue(
            ProfileSettingsRowRenderPolicy.usesDirectCompose(
                ProfileActionItem("Open printer web interface", "Printer dashboard", Runnable {}),
                false,
            ),
        )
        assertTrue(ProfileSettingsRowRenderPolicy.usesDirectCompose(SpaceItem(0, 4), false))
        assertTrue(ProfileSettingsRowRenderPolicy.usesDirectCompose(null, optionSupportsDirect = true))
    }

    @Test
    fun OrcaFallbackOptionRowsUseTheSameComposeList() {
        val fallback = ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileFallbackOption.create(
            title = "Host",
            fullTitle = "Printer host type",
            description = "Select the protocol",
            definition = ConfigOptionDef(),
            valueProvider = Supplier { "moonraker" },
            choices = listOf(ProfileOpenOptionResolver.Choice("Moonraker", "moonraker")),
            onChanged = Consumer {},
            onLongClick = Runnable {},
        )

        assertTrue(ProfileSettingsRowRenderPolicy.usesDirectCompose(fallback, false))
    }

    @Test
    fun anEmptyUnknownRowIsSkippedWithoutAViewFallback() {
        assertFalse(ProfileSettingsRowRenderPolicy.usesDirectCompose(null, optionSupportsDirect = false))
    }
}
