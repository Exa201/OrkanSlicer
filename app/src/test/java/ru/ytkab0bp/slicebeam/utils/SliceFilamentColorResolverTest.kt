package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SliceFilamentColorResolverTest {
    @Test fun nativeSlicePaletteFollowsEffectiveConfigInsteadOfStaleUserPalette() {
        assertArrayEquals(
            intArrayOf(0xFFFFFF, 0x0047BB),
            SliceFilamentColorResolver.resolve("#FFFFFF;#0047BB", 2, intArrayOf(0x212721, 0x0C0C0B)),
        )
    }

    @Test fun parsesOrcaQuotedColorVectorsAndPadsOnlyWhenNeeded() {
        assertArrayEquals(
            intArrayOf(0x112233, 0x445566, 0x445566),
            SliceFilamentColorResolver.resolve("\"#112233\";\"#445566\"", 3, intArrayOf()),
        )
    }

    @Test fun fallsBackToCurrentPaletteWhenConfigColorsAreInvalid() {
        assertArrayEquals(
            intArrayOf(0x123456, 0x123456),
            SliceFilamentColorResolver.resolve("not-a-color", 2, intArrayOf(0x123456)),
        )
    }

    @Test fun fillsMissingConfiguredToolColorsFromTheirMatchingAppSlots() {
        assertArrayEquals(
            intArrayOf(0xFF0000, 0x0047BB, 0x00AA00),
            SliceFilamentColorResolver.resolve("#FF0000", 3, intArrayOf(0x212721, 0x0047BB, 0x00AA00)),
        )
    }

    @Test fun previewKeepsThePaletteUsedForTheSliceInsteadOfTheGlobalPalette() {
        assertArrayEquals(
            intArrayOf(0x123456, 0xABCDEF),
            SliceFilamentColorResolver.previewColors(
                intArrayOf(0xFF123456.toInt(), 0xFFABCDEF.toInt()),
                intArrayOf(0x000000, 0xFFFFFF),
            ),
        )
    }

    @Test fun previewUsesGlobalPaletteOnlyWhenSlicePaletteIsUnavailable() {
        assertArrayEquals(
            intArrayOf(0x123456, 0xABCDEF),
            SliceFilamentColorResolver.previewColors(null, intArrayOf(0xFF123456.toInt(), 0xFFABCDEF.toInt())),
        )
    }

    @Test fun aMultiToolSliceUsesToolColorPreviewEvenWhenTheEditableModelLooksSingleColor() {
        assertEquals(true, SliceFilamentColorResolver.shouldUseToolPreview(false, intArrayOf(0x112233, 0x445566)))
        assertEquals(true, SliceFilamentColorResolver.shouldUseToolPreview(true, intArrayOf(0x112233)))
        assertEquals(false, SliceFilamentColorResolver.shouldUseToolPreview(false, intArrayOf(0x112233)))
    }
}