package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepImportOptionsTest {
    @Test fun recognizesStepAndStpCaseInsensitively() {
        assertTrue(StepImportOptions.isStepFile("part.step"))
        assertTrue(StepImportOptions.isStepFile("PART.STP"))
        assertFalse(StepImportOptions.isStepFile("part.stl"))
        assertFalse(StepImportOptions.isStepFile("part.step.backup"))
    }

    @Test fun defaultMeshingOptionsMatchOrcaDefaultsAndAreValid() {
        val options = StepImportOptions()
        assertEquals(0.003, options.linearDeflection, 0.0)
        assertEquals(0.5, options.angularDeflection, 0.0)
        assertFalse(options.splitCompound)
        assertNull(options.validate())
    }

    @Test fun rejectsNonFiniteAndOutOfRangeMeshOptions() {
        assertTrue(StepImportOptions(Double.NaN, 0.5, false).validate() != null)
        assertTrue(StepImportOptions(0.003, Double.POSITIVE_INFINITY, false).validate() != null)
        assertTrue(StepImportOptions(0.0, 0.5, false).validate() != null)
        assertTrue(StepImportOptions(0.003, 2.0, false).validate() != null)
        assertNull(StepImportOptions(1.0, 1.5, true).validate())
    }
}
