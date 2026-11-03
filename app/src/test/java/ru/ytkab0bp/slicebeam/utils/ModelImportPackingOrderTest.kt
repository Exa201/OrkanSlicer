package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelImportPackingOrderTest {
    @Test
    fun placesLargestFootprintsFirstAndKeepsEqualAndInvalidEntriesStable() {
        assertEquals(
            listOf(2, 0, 1, 3, 4),
            ModelImportPackingOrder.largestFirst(listOf(100.0, 100.0, 400.0, Double.NaN, 0.0)),
        )
    }

    @Test
    fun binaryStlPreflightUsesTheLargestAxisPairAsAnOrientationAwareFootprint() {
        val broad = binaryStl(floatArrayOf(10f, 0f, 0f, 0f, 8f, 0f, 0f, 0f, 1f))
        val tall = binaryStl(floatArrayOf(5f, 0f, 0f, 0f, 5f, 0f, 0f, 0f, 20f))
        val broadArea = StlImportPreflight.largestOrientedFootprintArea(
            ByteArrayInputStream(broad), broad.size.toLong(),
        )
        val tallArea = StlImportPreflight.largestOrientedFootprintArea(
            ByteArrayInputStream(tall), tall.size.toLong(),
        )

        assertEquals(80.0, broadArea!!, 1e-6)
        assertEquals(100.0, tallArea!!, 1e-6)
        assertEquals(listOf(1, 0), ModelImportPackingOrder.largestFirst(listOf(broadArea, tallArea)))
    }

    @Test
    fun asciiAndMalformedStlDoNotClaimAFootprint() {
        assertNull(StlImportPreflight.largestOrientedFootprintArea(
            ByteArrayInputStream("solid test\nendsolid test\n".toByteArray()),
        ))
        assertNull(StlImportPreflight.largestOrientedFootprintArea(ByteArrayInputStream(ByteArray(84))))
    }

    private fun binaryStl(vertices: FloatArray): ByteArray {
        require(vertices.size == 9)
        val output = ByteArrayOutputStream()
        val header = ByteArray(84)
        ByteBuffer.wrap(header, 80, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
        output.write(header)
        val facet = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
        repeat(3) { facet.putFloat(0f) }
        vertices.forEach(facet::putFloat)
        facet.putShort(0)
        output.write(facet.array())
        return output.toByteArray()
    }
}
