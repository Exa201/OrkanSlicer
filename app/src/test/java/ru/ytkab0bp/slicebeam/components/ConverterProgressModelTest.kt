package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConverterProgressModelTest {
    @Test
    fun stageProgressIsMonotonicAndBounded() {
        assertEquals(0, ConverterProgressModel.advanceStage(0, -10))
        assertEquals(70, ConverterProgressModel.advanceStage(70, 45))
        assertEquals(ConverterProgressModel.MAX_STAGE_PROGRESS, ConverterProgressModel.advanceStage(70, 200))
    }

    @Test
    fun estimateUsesHighestSourceAndClampsRunningProgress() {
        assertEquals(45, ConverterProgressModel.estimate(30, 45, 10_000).percent)
        assertEquals(ConverterProgressModel.MAX_RUNNING_PROGRESS, ConverterProgressModel.estimate(100, 120, 10_000).percent)
        assertEquals(0, ConverterProgressModel.estimate(-4, -2, -1).elapsedMillis)
    }

    @Test
    fun remainingTimeIsOmittedUntilProgressCanSupportAnEstimate() {
        assertNull(ConverterProgressModel.estimate(2, 0, 10_000).remainingMillis)
        assertEquals(388_000L, ConverterProgressModel.estimate(3, 0, 12_000).remainingMillis)
    }

    @Test
    fun stlFileProgressTracksPartialAndCompleteBinaryFiles() {
        val facets = 200L
        val expectedLength = 84L + facets * 50L

        assertEquals(0, ConverterProgressModel.stlFileProgress(83, facets))
        assertEquals(0, ConverterProgressModel.stlFileProgress(expectedLength, 0))
        assertEquals(50, ConverterProgressModel.stlFileProgress(84L + facets * 25L, facets))
        assertEquals(92, ConverterProgressModel.stlFileProgress(expectedLength, facets))
        assertEquals(92, ConverterProgressModel.stlFileProgress(expectedLength * 2, facets))
    }

    @Test
    fun elapsedAndRemainingTimesFormatAsMinutesOrHours() {
        assertEquals("00:00", ConverterProgressModel.formatTime(-1))
        assertEquals("01:05", ConverterProgressModel.formatTime(65_000))
        assertEquals("1:00:01", ConverterProgressModel.formatTime(3_601_000))
    }
}
