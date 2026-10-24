package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
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
}
