package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelImportFileNameTest {
    @Test
    fun stepMimeTypeSuppliesExtensionWhenProviderNameIsGeneric() {
        assertEquals("assembly.step", ModelImportFileName.resolve("assembly", "/document/42", "model/step"))
        assertEquals("assembly.step", ModelImportFileName.resolve("assembly", null, "application/x-step; charset=utf-8"))
        assertEquals("assembly.bin.step", ModelImportFileName.resolve("assembly.bin", null, "application/step"))
        assertEquals("assembly.custom.step", ModelImportFileName.resolve("assembly.custom", null, "model/step"))
    }

    @Test
    fun keepsStepAndStpExtensionsAndUsesPathWhenDisplayNameIsMissing() {
        assertEquals("part.STP", ModelImportFileName.resolve("part.STP", null, "model/step"))
        assertEquals("gear.stp", ModelImportFileName.resolve(null, "/document/gear.stp", "application/step"))
    }

    @Test
    fun doesNotReclassifyOtherFilesOrNamesWithAnotherExtension() {
        assertEquals("part.stl", ModelImportFileName.resolve("part.stl", null, "model/step"))
        assertEquals("part.stl", ModelImportFileName.resolve("part.stl", null, "model/stl"))
        assertEquals(null, ModelImportFileName.resolve(null, null, "application/octet-stream"))
    }

    @Test
    fun genericProviderNamesCanUseTheStepHeaderFallback() {
        assertEquals("download.step", ModelImportFileName.withStepExtension("download"))
        assertEquals("download.step", ModelImportFileName.withStepExtension("download.bin"))
        assertEquals("part.STP", ModelImportFileName.withStepExtension("part.STP"))
        assertEquals("model.step", ModelImportFileName.withStepExtension(null))
        assertTrue(ModelImportFileName.hasStepHeader("ISO-10303-21;\nHEADER;".toByteArray()))
        assertTrue(ModelImportFileName.hasStepHeader("\uFEFF  ISO-10303-21;".toByteArray()))
        assertFalse(ModelImportFileName.hasStepHeader("solid mesh".toByteArray()))
    }
}
