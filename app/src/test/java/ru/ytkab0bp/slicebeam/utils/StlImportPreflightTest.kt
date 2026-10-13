package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class StlImportPreflightTest {
    private fun stl(triangles: Int, actualLength: Long = 84L + 50L * triangles): File {
        val file = File.createTempFile("import-preflight-", ".stl")
        RandomAccessFile(file, "rw").use { output ->
            output.seek(80)
            output.writeInt(Integer.reverseBytes(triangles))
            output.setLength(actualLength)
        }
        return file
    }

    @Test fun warnsOnlyForLargeValidBinaryStl() {
        val large = stl(500_001)
        val small = stl(500_000)
        val malformed = stl(500_001, 84L)
        try {
            assertEquals(500_001L, StlImportPreflight.largeTriangleCount(large))
            assertNull(StlImportPreflight.largeTriangleCount(small))
            assertNull(StlImportPreflight.largeTriangleCount(malformed))
        } finally {
            large.delete()
            small.delete()
            malformed.delete()
        }
    }
}
