package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDecodePolicyTest {
    @Test
    fun keepsSmallImagesAtFullResolution() {
        assertEquals(1, ImageDecodePolicy.sampleSize(1024, 768))
        assertEquals(1, ImageDecodePolicy.sampleSize(640, 480))
    }

    @Test
    fun downsamplesLargeImagesToUploadScale() {
        assertEquals(2, ImageDecodePolicy.sampleSize(2048, 1536))
        assertEquals(4, ImageDecodePolicy.sampleSize(4000, 3000))
    }

    @Test
    fun invalidDimensionsDoNotCreateInvalidSampleSizes() {
        assertEquals(1, ImageDecodePolicy.sampleSize(0, 100))
        assertEquals(1, ImageDecodePolicy.sampleSize(100, 100, 0))
    }
}
