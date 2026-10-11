package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class SliceLayersValuesTest {
    @Test
    fun nativeRangeIsDisplayedAsOneBasedLayerNumbers() {
        assertEquals(SliceLayerRange(from = 1, to = 10, maximum = 10), normalizeSliceLayerRange(10, 0, 9))
        assertEquals(SliceLayerRange(from = 3, to = 7, maximum = 10), normalizeSliceLayerRange(10, 6, 2))
    }

    @Test
    fun emptyAndOutOfRangeValuesAreClampedToExistingLayers() {
        assertEquals(SliceLayerRange(from = 1, to = 1, maximum = 1), normalizeSliceLayerRange(0, -20, 20))
        assertEquals(SliceLayerRange(from = 5, to = 5, maximum = 5), normalizeSliceLayerRange(5, 99, 100))
        assertEquals(SliceLayerRange(from = 1, to = 1, maximum = 5), normalizeSliceLayerRange(5, -10, -1))
    }

    @Test
    fun nativeLongExtremesCannotOverflowDuringOneBasedConversion() {
        assertEquals(
            SliceLayerRange(from = 1, to = Int.MAX_VALUE, maximum = Int.MAX_VALUE),
            normalizeSliceLayerRange(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE),
        )
    }

    @Test
    fun sliderFractionsMapToOneBasedLayersAndClampToTheTrack() {
        assertEquals(1, layerFromSliderFraction(-1f, 10))
        assertEquals(6, layerFromSliderFraction(0.5f, 10))
        assertEquals(10, layerFromSliderFraction(1f, 10))
        assertEquals(10, layerFromSliderFraction(2f, 10))
        assertEquals(1, layerFromSliderFraction(0.7f, 1))
    }

    @Test
    fun sliderFractionAndLayerRoundTripAcrossLargeLayerCounts() {
        val layer = 43
        val fraction = layerSliderFraction(layer, 100)

        assertEquals(layer, layerFromSliderFraction(fraction, 100))
        assertEquals(0f, layerSliderFraction(1, Int.MAX_VALUE), 0f)
        assertEquals(1f, layerSliderFraction(Int.MAX_VALUE, Int.MAX_VALUE), 0f)
        assertEquals(1_073_741_824, layerFromSliderFraction(0.5f, Int.MAX_VALUE))
    }

    @Test
    fun sliderEndpointsStayOrderedAndWithinThePrintableLayerRange() {
        val range = SliceLayerRange(from = 4, to = 8, maximum = 10)

        assertEquals(SliceLayerRange(from = 8, to = 8, maximum = 10), withSliceLayerFrom(range, 99))
        assertEquals(SliceLayerRange(from = 4, to = 4, maximum = 10), withSliceLayerTo(range, -5))
        assertEquals(SliceLayerRange(from = 1, to = 10, maximum = 10), withSliceLayerFrom(range, -5).let {
            withSliceLayerTo(it, 100)
        })
    }
}
