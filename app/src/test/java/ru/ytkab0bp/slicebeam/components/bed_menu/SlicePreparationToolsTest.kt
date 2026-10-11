package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class SlicePreparationToolsTest {
    @Test
    fun detectsAnycubicKobraThreeModelNamesWithoutCaseSensitivity() {
        assertTrue(SlicePreparationTools.isAnycubicKobra3Model("Anycubic Kobra 3 V2"))
        assertTrue(SlicePreparationTools.isAnycubicKobra3Model("anycubic kobra 3 max"))
        assertFalse(SlicePreparationTools.isAnycubicKobra3Model("Anycubic Kobra 2"))
        assertFalse(SlicePreparationTools.isAnycubicKobra3Model(null))
    }

    @Test
    fun acceptsOnlyConfiguredPrinterUploadProtocolsAndNonEmptyHosts() {
        assertTrue(SlicePreparationTools.isSendTarget(null, "printer.local"))
        assertTrue(SlicePreparationTools.isSendTarget("moonraker", "192.168.1.4"))
        assertTrue(SlicePreparationTools.isSendTarget("klipper", "host"))
        assertTrue(SlicePreparationTools.isSendTarget("octoprint", " "))
        assertFalse(SlicePreparationTools.isSendTarget("unsupported", "host"))
        assertFalse(SlicePreparationTools.isSendTarget("moonraker", ""))
        assertFalse(SlicePreparationTools.isSendTarget("klipper", null))
    }

    @Test
    fun detectsAutoLevelCommandInStartGcodeWithoutMatchingItsNameAsPartOfAnotherWord() {
        assertTrue(SlicePreparationTools.hasAutomaticBedLeveling("G28\nG29\nM104 S0"))
        assertTrue(SlicePreparationTools.hasAutomaticBedLeveling("g29 ; level bed"))
        assertFalse(SlicePreparationTools.hasAutomaticBedLeveling("G28\nM104 S0"))
        assertFalse(SlicePreparationTools.hasAutomaticBedLeveling("M290 Z0.2"))
        assertFalse(SlicePreparationTools.hasAutomaticBedLeveling(null))
    }

    @Test
    fun printerSelectionChangeIsComparedToTheActiveProfileTitle() {
        assertTrue(SlicePreparationTools.didPrinterChange("Kobra 3", "Qidi Q2"))
        assertFalse(SlicePreparationTools.didPrinterChange("Kobra 3", "Kobra 3"))
        assertTrue(SlicePreparationTools.didPrinterChange(null, "Custom printer"))
    }

    @Test
    fun changedPrinterMustBeActivatedBeforeTheJobCanBeResliced() {
        var activationAttempts = 0
        assertEquals(
            SlicePreparationTools.PrinterActivationResult.FAILED,
            SlicePreparationTools.activateChangedPrinter("Kobra 3", "Qidi Q2") {
                activationAttempts++
                false
            },
        )
        assertEquals(1, activationAttempts)
        assertEquals(
            SlicePreparationTools.PrinterActivationResult.ACTIVATED,
            SlicePreparationTools.activateChangedPrinter("Kobra 3", "Qidi Q2") { true },
        )
        assertEquals(
            SlicePreparationTools.PrinterActivationResult.UNCHANGED,
            SlicePreparationTools.activateChangedPrinter("Kobra 3", "Kobra 3") {
                error("An unchanged printer must not be activated again")
            },
        )
    }

    @Test
    fun scansTheHighestStandaloneToolNumberAndIgnoresCommentsAndInlineCommands() {
        val file = File.createTempFile("slice-tools", ".gcode")
        try {
            file.writeText("""
                ; T9
                T0
                G1 X3 T8
                T2 ; switch material
            """.trimIndent())
            assertEquals(2, SlicePreparationTools.readMaxTool(file))
            assertArrayEquals(intArrayOf(0, 2), SlicePreparationTools.readUsedTools(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun showsOnlyT1ForAnImportedSecondFilamentJob() {
        val file = File.createTempFile("slice-t1-only", ".gcode")
        try {
            file.writeText("; T0 is unused\nT1 ; change extruder\nG1 X1 E0.2\n")
            assertArrayEquals(intArrayOf(1), SlicePreparationTools.readUsedTools(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun readsSlicedMaterialInsteadOfAChangedPalette() {
        val file = File.createTempFile("slice-material", ".gcode")
        try {
            file.writeText("T1 ; change extruder\nM104 S205\n; filament_type = PETG;PLA\n")
            assertEquals(listOf("PETG", "PLA"), SlicePreparationTools.readFilamentTypes(file))
            assertArrayEquals(intArrayOf(1), SlicePreparationTools.readUsedTools(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun unknownOrDifferentLoadedMaterialCannotStartPrinting() {
        assertTrue(SlicePreparationTools.materialMatchesSlicedFile("PLA", "pla"))
        assertFalse(SlicePreparationTools.materialMatchesSlicedFile("PLA", "PETG"))
        assertFalse(SlicePreparationTools.materialMatchesSlicedFile("", "PLA"))
        assertFalse(SlicePreparationTools.materialMatchesSlicedFile("PLA", null))
    }

    @Test(expected = IOException::class)
    fun rejectsAnEmptyGcodeFileBeforeShowingPrintPreparation() {
        val file = File.createTempFile("slice-tools-empty", ".gcode")
        try {
            SlicePreparationTools.readMaxTool(file)
        } finally {
            file.delete()
        }
    }
}
