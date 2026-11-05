package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaConditionalSettingsPolicyTest {
    @Test
    fun lightningOptionsRequireLightningPatternAndNonzeroDensity() {
        assertTrue(OrcaConditionalSettingsPolicy.showLightningOptions("15%", "lightning"))
        assertTrue(OrcaConditionalSettingsPolicy.showLightningOptions("0,5", "Lightning"))
        assertFalse(OrcaConditionalSettingsPolicy.showLightningOptions("0%", "lightning"))
        assertFalse(OrcaConditionalSettingsPolicy.showLightningOptions("15%", "grid"))
        assertFalse(OrcaConditionalSettingsPolicy.showLightningOptions(null, "lightning"))
        assertFalse(OrcaConditionalSettingsPolicy.showLightningOptions("invalid", "lightning"))
    }

    @Test
    fun onlyLightningSectionFollowsTheConditionalVisibilityRule() {
        assertFalse(OrcaConditionalSettingsPolicy.showSection("Lightning infill", false))
        assertTrue(OrcaConditionalSettingsPolicy.showSection("Lightning infill", true))
        assertTrue(OrcaConditionalSettingsPolicy.showSection("Infill", false))
    }
}
