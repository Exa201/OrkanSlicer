package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilamentSlotTest {
    @Test
    fun constructorKeepsJavaFieldContractAndForcesOpaqueArgbColor() {
        val slot = FilamentSlot(0x00123456, "PLA")

        assertEquals(0xFF123456.toInt(), slot.color)
        assertEquals("PLA", slot.type)
        assertEquals(Int::class.javaPrimitiveType, FilamentSlot::class.java.getField("color").type)
        assertEquals(String::class.java, FilamentSlot::class.java.getField("type").type)
        assertEquals(2, FilamentSlot::class.java.constructors.single().parameterCount)
    }

    @Test
    fun fieldsRemainWritableAndMaterialMayBeMissing() {
        val slot = FilamentSlot(0xFF000000.toInt(), "PETG")

        FilamentSlot::class.java.getField("color").setInt(slot, 0xFFABCDEF.toInt())
        FilamentSlot::class.java.getField("type").set(slot, null)

        assertEquals(0xFFABCDEF.toInt(), slot.color)
        assertNull(slot.type)
        assertTrue(FilamentSlot::class.java.getField("color").modifiers and 0x0001 != 0)
    }
}
