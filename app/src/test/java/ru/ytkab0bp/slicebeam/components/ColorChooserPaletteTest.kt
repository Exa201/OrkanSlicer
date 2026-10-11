package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.CssColors

class ColorChooserPaletteTest {
    @Test
    fun commonColorsRemainCompleteAndInTheDeclaredOrder() {
        val expected = CssColors.COMMON.mapNotNull { CssColors.ALL[it] }

        assertEquals(18, ColorChooserPalette.commonColors().size)
        assertEquals(expected, ColorChooserPalette.commonColors())
    }

    @Test
    fun cssPaletteRemainsComprehensiveAndOpaque() {
        val colors = ColorChooserPalette.allColors()

        assertTrue("CSS palette should retain the standard named colors", colors.size >= 140)
        assertEquals(CssColors.ALL.size, colors.size)
        assertTrue(colors.all { it ushr 24 == 0xFF })
    }

    @Test
    fun swatchColumnsAdaptToPhoneAndTabletWidths() {
        assertEquals(2, ColorChooserPalette.columnsForWidth(120))
        assertEquals(3, ColorChooserPalette.columnsForWidth(200))
        assertEquals(5, ColorChooserPalette.columnsForWidth(320))
        assertEquals(6, ColorChooserPalette.columnsForWidth(360))
        assertEquals(8, ColorChooserPalette.columnsForWidth(900))
    }
}
