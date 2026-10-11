package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class LineWidthConversionTest {
    @Test fun percentAndMillimetresStayCoupledToNozzle() {
        assertEquals(105.0, LineWidthConversion.percent("0.42", 0.4)!!, 0.0001)
        assertEquals(0.42, LineWidthConversion.millimetres(105.0, 0.4), 0.0001)
        assertEquals(105.0, LineWidthConversion.percentFromMillimetres(0.63, 0.6), 0.0001)
        assertEquals("105", LineWidthConversion.formatPercent(105.0))
        assertEquals("0.42", LineWidthConversion.formatMillimetres(0.42))
    }

    @Test fun percentageRemainsRelativeWhenNozzleChanges() {
        assertEquals(0.42, LineWidthConversion.millimetres(LineWidthConversion.percent("105%", 0.4)!!, 0.4), 0.0001)
        assertEquals(0.63, LineWidthConversion.millimetres(LineWidthConversion.percent("105%", 0.6)!!, 0.6), 0.0001)
    }
}
