package ru.ytkab0bp.slicebeam.utils

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastenerStlWriterTest {
    @Test fun m12BoltWritesValidBinaryStl() = assertValidBinaryStl { file ->
        FastenerStlWriter.writeBolt(file, diameter = 12f, pitch = 1.75f, length = 40f,
            headAcrossFlats = 19f, headHeight = 8f)
    }

    @Test fun threadedNutWritesValidBinaryStl() = assertValidBinaryStl { file ->
        FastenerStlWriter.writeThreadedNut(file, diameter = 6f, pitch = 1f,
            acrossFlats = 10f, height = 5f, radialClearance = .15f)
    }

    @Test fun washerWritesValidBinaryStl() = assertValidBinaryStl { file ->
        FastenerStlWriter.writeWasher(file, outerDiameter = 12f, boreDiameter = 6f, thickness = 2f)
    }

    private fun assertValidBinaryStl(write: (File) -> Int) {
        val file = File.createTempFile("orkanslicer-fastener-", ".stl")
        try {
            val writtenFacets = write(file)
            val bytes = file.readBytes()
            assertTrue("generated mesh must contain facets", writtenFacets > 0)
            assertEquals("binary STL length must match its facet count", 84L + writtenFacets * 50L, bytes.size.toLong())
            val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(writtenFacets, data.getInt(80))
            data.position(84)
            repeat(writtenFacets) {
                val nx = data.float
                val ny = data.float
                val nz = data.float
                assertTrue("facet normal must be finite", nx.isFinite() && ny.isFinite() && nz.isFinite())
                val normalLength = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
                assertTrue("facet normal should be normalized", normalLength in .99f..1.01f)
                repeat(9) { assertTrue("vertex coordinates must be finite", data.float.isFinite()) }
                assertEquals("STL attribute bytes should be zero", 0, data.short.toInt())
            }
        } finally {
            file.delete()
        }
    }
}
