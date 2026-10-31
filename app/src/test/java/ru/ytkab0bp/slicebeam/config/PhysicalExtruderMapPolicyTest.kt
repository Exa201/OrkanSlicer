package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalExtruderMapPolicyTest {
    @Test fun acceptsOrcaAfcExampleWithSeveralLogicalSlotsOnOneHead() {
        assertArrayEquals(
            intArrayOf(0, 0, 0, 0, 1, 2, 3),
            PhysicalExtruderMapPolicy.parse("0,0,0,0,1,2,3", 7),
        )
    }

    @Test fun rejectsMissingOrExtraEntries() {
        assertFalse(PhysicalExtruderMapPolicy.isValid("0,1", 3))
        assertFalse(PhysicalExtruderMapPolicy.isValid("0,1,2,3", 3))
    }

    @Test fun rejectsNegativeOutOfRangeAndNonIntegerHeadIds() {
        assertFalse(PhysicalExtruderMapPolicy.isValid("0,-1", 2))
        assertFalse(PhysicalExtruderMapPolicy.isValid("0,2", 2))
        assertFalse(PhysicalExtruderMapPolicy.isValid("0,one", 2))
    }

    @Test fun identityMappingUsesOneEntryPerLogicalExtruder() {
        assertEquals("0,1,2,3", PhysicalExtruderMapPolicy.identity(4))
        assertTrue(PhysicalExtruderMapPolicy.isValid(PhysicalExtruderMapPolicy.identity(4), 4))
    }
}
