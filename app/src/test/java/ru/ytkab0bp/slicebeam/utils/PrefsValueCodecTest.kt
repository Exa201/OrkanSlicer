package ru.ytkab0bp.slicebeam.utils

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsValueCodecTest {
    @Test
    fun emptyAndFullyMalformedPalettesKeepOneUsableDefaultSlot() {
        listOf(null, "", ",,").forEach { raw ->
            val slots = PrefsValueCodec.parseFilamentSlots(raw)
            assertEquals(1, slots.size)
            assertEquals("PLA", slots.single().type)
            assertEquals(0xFF1AC5A2.toInt(), slots.single().color)
        }
    }

    @Test
    fun paletteAcceptsLegacyColorsMaterialsMalformedHexAndCapsAtSixteenSlots() {
        val slots = PrefsValueCodec.parseFilamentSlots("123456,PETG|FF0000,ABS|broken")

        assertEquals(3, slots.size)
        assertEquals("PLA", slots[0].type)
        assertEquals(0xFF123456.toInt(), slots[0].color)
        assertEquals("PETG", slots[1].type)
        assertEquals(0xFFFF0000.toInt(), slots[1].color)
        assertEquals("ABS", slots[2].type)
        assertEquals(0xFF1AC5A2.toInt(), slots[2].color)

        val oversized = (0 until 20).joinToString(",") { "PLA|${it.toString(16).padStart(6, '0')}" }
        assertEquals(Prefs.MAX_FILAMENT_COLORS, PrefsValueCodec.parseFilamentSlots(oversized).size)
    }

    @Test
    fun serializationRetainsMaterialAndOpaqueSixDigitColors() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val encoded = PrefsValueCodec.serializeFilamentSlots(
                listOf(FilamentSlot(0x00123456, "PETG"), FilamentSlot(0xFF00ABCD.toInt(), "PLA")),
            )
            assertEquals("PETG|123456,PLA|00ABCD", encoded)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun emptySlotRoundTripsWithoutChangingItsIndex() {
        val original = listOf(
            FilamentSlot(0xFF112233.toInt(), "PLA"),
            FilamentSlot(0xFF808080.toInt(), null),
            FilamentSlot(0xFF445566.toInt(), "PETG"),
        )

        val restored = PrefsValueCodec.parseFilamentSlots(PrefsValueCodec.serializeFilamentSlots(original))

        assertEquals(listOf("PLA", null, "PETG"), restored.map { it.type })
        assertEquals(original.map { it.color }, restored.map { it.color })
    }

    @Test
    fun unknownThemeOrdinalsFallBackToSystem() {
        assertEquals(Prefs.ThemeMode.SYSTEM, PrefsValueCodec.themeMode(-1))
        assertEquals(Prefs.ThemeMode.LIGHT, PrefsValueCodec.themeMode(1))
        assertEquals(Prefs.ThemeMode.DARK, PrefsValueCodec.themeMode(2))
        assertEquals(Prefs.ThemeMode.SYSTEM, PrefsValueCodec.themeMode(999))
    }
}
