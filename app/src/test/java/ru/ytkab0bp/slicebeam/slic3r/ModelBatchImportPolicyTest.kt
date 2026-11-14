package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelBatchImportPolicyTest {
    @Test
    fun acceptsOrcaIndependentMeshFormatsCaseInsensitively() {
        listOf("part.stl", "part.OBJ", "part.amf", "part.oltp").forEach {
            assertTrue("Expected $it to support batch import", ModelBatchImportPolicy.supports(it))
        }
    }

    @Test
    fun keepsProjectAndOptionDrivenFormatsOutOfFlatBatchImport() {
        listOf("project.3mf", "part.step", "part.stp", "shape.svg", "preview.gcode", null, " ").forEach {
            assertFalse("Expected $it to stay on its specialized import route", ModelBatchImportPolicy.supports(it))
        }
    }
}
