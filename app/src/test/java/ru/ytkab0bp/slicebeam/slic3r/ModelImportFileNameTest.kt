package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelImportFileNameTest {
    @Test
    fun stepMimeTypeSuppliesExtensionWhenProviderNameIsGeneric() {
        assertEquals("assembly.step", ModelImportFileName.resolve("assembly", "/document/42", "model/step"))
        assertEquals("assembly.step", ModelImportFileName.resolve("assembly", null, "application/x-step; charset=utf-8"))
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
}
