package ru.ytkab0bp.slicebeam.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMenuLayoutPolicyTest {
    @Test
    fun phoneWindowsUseCompactSettingsSpacing() {
        assertTrue(SettingsMenuLayoutPolicy.usesCompactLayout(360, 800))
        assertTrue(SettingsMenuLayoutPolicy.usesCompactLayout(599, 1000))
    }

    @Test
    fun shortLandscapePhoneUsesCompactSettingsSpacing() {
        assertTrue(SettingsMenuLayoutPolicy.usesCompactLayout(850, 400))
        assertTrue(SettingsMenuLayoutPolicy.usesCompactLayout(840, 479))
    }

    @Test
    fun shortWideWindowUsesTwoColumns() {
        assertTrue(SettingsMenuLayoutPolicy.usesTwoColumnLayout(800, 400))
        assertTrue(SettingsMenuLayoutPolicy.usesTwoColumnLayout(1280, 479))
    }

    @Test
    fun portraitAndTallTabletWindowsKeepSingleColumn() {
        assertFalse(SettingsMenuLayoutPolicy.usesTwoColumnLayout(599, 900))
        assertFalse(SettingsMenuLayoutPolicy.usesTwoColumnLayout(840, 480))
        assertFalse(SettingsMenuLayoutPolicy.usesTwoColumnLayout(1200, 900))
    }

    @Test
    fun tabletSizedWindowsWithEnoughHeightUseExpandedSpacing() {
        assertFalse(SettingsMenuLayoutPolicy.usesCompactLayout(600, 800))
        assertFalse(SettingsMenuLayoutPolicy.usesCompactLayout(840, 480))
        assertFalse(SettingsMenuLayoutPolicy.usesCompactLayout(1200, 900))
    }

    @Test
    fun missingWindowBoundsDoNotUseCompactSpacing() {
        assertFalse(SettingsMenuLayoutPolicy.usesCompactLayout(0, 0))
        assertFalse(SettingsMenuLayoutPolicy.usesCompactLayout(-1, -1))
    }
}
