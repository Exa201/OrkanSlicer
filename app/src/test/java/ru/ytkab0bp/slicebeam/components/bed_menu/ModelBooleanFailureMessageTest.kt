package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.R

class ModelBooleanFailureMessageTest {
    @Test
    fun nativeFailureStatusesHaveDistinctActionableMessages() {
        val statuses = -2 downTo -7
        val resources = statuses.map { status ->
            ModelBooleanFailureMessage.resourceFor(status)
                ?: throw AssertionError("status $status should have a specific message")
        }

        assertEquals("every reported native failure should be distinguishable", resources.size, resources.toSet().size)
        assertEquals(R.string.ModelBooleanUnsupportedInstances, resources[0])
        assertEquals(R.string.ModelBooleanInvalidMesh, resources[1])
        assertEquals(R.string.ModelBooleanInvalidResult, resources[2])
        assertEquals(R.string.ModelBooleanResultCreationFailed, resources[3])
        assertEquals(R.string.ModelBooleanEngineFailed, resources[4])
        assertEquals(R.string.ModelBooleanNoCut, resources[5])
    }

    @Test
    fun genericOrUnknownFailuresKeepTheExistingObjectNameFallback() {
        assertNull(ModelBooleanFailureMessage.resourceFor(-1))
        assertNull(ModelBooleanFailureMessage.resourceFor(Int.MIN_VALUE))
        assertNotEquals(R.string.ModelBooleanFailed, ModelBooleanFailureMessage.resourceFor(-2))
    }
}
