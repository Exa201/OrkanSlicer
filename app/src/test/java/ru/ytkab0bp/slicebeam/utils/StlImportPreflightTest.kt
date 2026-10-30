package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class StlImportPreflightTest {
    private fun stl(triangles: Int, actualLength: Long = 84L + 50L * triangles): File {
        val file = File.createTempFile("import-preflight-", ".stl")
        try {
            RandomAccessFile(file, "rw").use { output ->
                output.seek(80)
                output.writeInt(Integer.reverseBytes(triangles))
                output.setLength(actualLength)
            }
            return file
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    @Test fun warnsOnlyForLargeValidBinaryStl() {
        val small = stl(6)
        val malformed = stl(6, 84L)
        try {
            assertEquals(500_001L, StlImportPreflight.largeTriangleCount(
                triangleCount = 500_001L,
                fileLength = 84L + 50L * 500_001L,
            ))
            assertNull(StlImportPreflight.largeTriangleCount(
                triangleCount = 500_000L,
                fileLength = 84L + 50L * 500_000L,
            ))
            assertNull(StlImportPreflight.largeTriangleCount(
                triangleCount = 500_001L,
                fileLength = 84L,
            ))
            assertEquals(6L, StlImportPreflight.largeTriangleCount(small, limit = 5L))
            assertNull(StlImportPreflight.largeTriangleCount(malformed, limit = 5L))
        } finally {
            small.delete()
            malformed.delete()
        }
    }
}
