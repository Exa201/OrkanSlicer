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

    @Test
    fun infillAnchorsRequireSparseInfillAndACompatiblePattern() {
        assertTrue(OrcaConditionalSettingsPolicy.showInfillAnchorOptions("15%", "grid"))
        assertTrue(OrcaConditionalSettingsPolicy.showInfillAnchorOptions("0,5", "lightning"))
        assertFalse(OrcaConditionalSettingsPolicy.showInfillAnchorOptions("0%", "grid"))
        assertFalse(OrcaConditionalSettingsPolicy.showInfillAnchorOptions("15%", "concentric"))
        assertFalse(OrcaConditionalSettingsPolicy.showInfillAnchorOptions("15%", "spiralinset"))
        assertFalse(OrcaConditionalSettingsPolicy.showInfillAnchorOptions(null, "grid"))
    }

    @Test
    fun onlyAnchorSectionFollowsTheAnchorVisibilityRule() {
        assertFalse(OrcaConditionalSettingsPolicy.showSection("Infill anchors", true, false))
        assertTrue(OrcaConditionalSettingsPolicy.showSection("Infill anchors", false, true))
        assertTrue(OrcaConditionalSettingsPolicy.showSection("Flush into infill", false, false))
    }
}
