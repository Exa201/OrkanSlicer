package ru.ytkab0bp.slicebeam.utils

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** Reads only the fixed binary STL header; the native importer still validates the mesh. */
object StlImportPreflight {
    fun largeTriangleCount(file: File, limit: Long = 500_000L): Long? {
        if (!file.name.endsWith(".stl", ignoreCase = true) || file.length() < 84L) return null
        return try {
            RandomAccessFile(file, "r").use { input ->
                input.seek(80)
                val count = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                if (count > limit && file.length() == 84L + 50L * count) count else null
            }
        } catch (_: IOException) { null }
    }
}
