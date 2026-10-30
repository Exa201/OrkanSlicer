package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertFalse(SlicePreparationTools.isSendTarget("octoprint", " "))
        assertFalse(SlicePreparationTools.isSendTarget("moonraker", "ftp://printer"))
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
    fun readsActualFirstToolUseOrderWhileKeepingDistinctToolsSorted() {
        val file = File.createTempFile("slice-color-order", ".gcode")
        try {
            file.writeText("; T9 is only a comment\nT0\nT2\nG1 X2 E0.1\nT1\nT0\n")
            val usage = SlicePreparationTools.readToolUsage(file)
            assertArrayEquals(intArrayOf(0, 1, 2), usage.used)
            assertArrayEquals(intArrayOf(0, 2, 1), usage.firstAppearance)
        } finally {
            file.delete()
        }
    }

    @Test
    fun combinedPreflightMatchesSeparateToolAndPressureAdvanceReaders() {
        val file = File.createTempFile("slice-preflight", ".gcode")
        try {
            file.writeText("""
                ; T9 is only a comment
                T0
                SET_PRESSURE_ADVANCE ADVANCE=0.11
                T2
                G1 X2 E0.1
                T1
                SET_PRESSURE_ADVANCE ADVANCE=0.04; Override pressure advance value
                T0
            """.trimIndent())
            val preflight = SlicePreparationTools.readPreflight(file)
            assertArrayEquals(intArrayOf(0, 1, 2), preflight.toolUsage.used)
            assertArrayEquals(intArrayOf(0, 2, 1), preflight.toolUsage.firstAppearance)
            assertEquals(
                SlicePreparationTools.readPressureAdvanceCommands(file),
                preflight.pressureAdvance,
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun warnsForSparseAceSlotsInTheThreeColorKobraJob() {
        val tools = intArrayOf(0, 1, 2)
        val toolCount = 3
        assertArrayEquals(
            intArrayOf(2),
            SlicePreparationTools.emptyAceSlotsBeforeUsedGate(tools, toolCount, intArrayOf(0, 2, 3)),
        )
        assertArrayEquals(
            intArrayOf(),
            SlicePreparationTools.emptyAceSlotsBeforeUsedGate(tools, toolCount, intArrayOf(0, 1, 2, 3)),
        )
        assertArrayEquals(
            intArrayOf(),
            SlicePreparationTools.emptyAceSlotsBeforeUsedGate(intArrayOf(0), toolCount, intArrayOf(0, 2, 3)),
        )
        assertArrayEquals(
            intArrayOf(),
            SlicePreparationTools.emptyAceSlotsBeforeUsedGate(tools, toolCount, intArrayOf()),
        )
    }

    @Test
    fun catchesASelectedGateThatBecameEmptyWithoutRejectingSparseValidGates() {
        val tools = intArrayOf(0, 1, 2)
        val valid = intArrayOf(0, 2, 3)
        assertNull(SlicePreparationTools.firstUnavailableAceTool(tools, intArrayOf(0, 2, 3), valid))
        assertEquals(1, SlicePreparationTools.firstUnavailableAceTool(tools, intArrayOf(0, 1, 3), valid))
        assertEquals(2, SlicePreparationTools.firstUnavailableAceTool(tools, intArrayOf(0, 2), valid))
        assertEquals(0, SlicePreparationTools.firstUnavailableAceTool(tools, null, valid))
    }

    @Test
    fun warnsWhenThreeModelToolsShareOnlyTwoPhysicalAceSlots() {
        val tools = intArrayOf(0, 1, 2)
        assertEquals(2, SlicePreparationTools.sharedAceGateCount(tools, intArrayOf(0, 0, 3)))
        assertEquals(1, SlicePreparationTools.sharedAceGateCount(tools, intArrayOf(0, 0, 0)))
        assertNull(SlicePreparationTools.sharedAceGateCount(tools, intArrayOf(0, 2, 3)))
        assertNull(SlicePreparationTools.sharedAceGateCount(intArrayOf(0), intArrayOf(0)))
        assertNull(SlicePreparationTools.sharedAceGateCount(tools, intArrayOf(0, 2)))
        assertNull(SlicePreparationTools.sharedAceGateCount(tools, null))
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

    @Test
    fun reportsExecutablePressureAdvanceSeparatelyFromMetadataAndStartCode() {
        val file = File.createTempFile("slice-pressure-advance", ".gcode")
        try {
            file.writeText("""
                ; pressure_advance = 0.09,0.10
                ; SET_PRESSURE_ADVANCE ADVANCE=0.11
                M900 K0.051
                SET_PRESSURE_ADVANCE ADVANCE=0.04; Override pressure advance value
                G1 X10 Y10
                SET_PRESSURE_ADVANCE ADVANCE=0.06; Override pressure advance value
                SET_PRESSURE_ADVANCE ADVANCE=0.04; Override pressure advance value
            """.trimIndent())
            assertEquals(
                SlicePreparationTools.PressureAdvanceCommands(
                    filament = listOf("0.04", "0.06"),
                    custom = listOf("0.051"),
                ),
                SlicePreparationTools.readPressureAdvanceCommands(file),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun metadataAloneDoesNotCountAsPressureAdvanceCommand() {
        val file = File.createTempFile("slice-pressure-metadata", ".gcode")
        try {
            file.writeText("; pressure_advance = 0.05\n; enable_pressure_advance = 1\n")
            assertEquals(
                SlicePreparationTools.PressureAdvanceCommands(emptyList(), emptyList()),
                SlicePreparationTools.readPressureAdvanceCommands(file),
            )
        } finally {
            file.delete()
        }
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
