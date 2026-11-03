package ru.ytkab0bp.slicebeam.utils

import java.io.File
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.IOException
import java.io.RandomAccessFile

/** Reads only the fixed binary STL header; the native importer still validates the mesh. */
object StlImportPreflight {
    private const val MAX_PREFLIGHT_FACETS = 2_000_000L

    fun largeTriangleCount(file: File, limit: Long = 500_000L): Long? {
        if (!file.name.endsWith(".stl", ignoreCase = true) || file.length() < 84L) return null
        return try {
            RandomAccessFile(file, "r").use { input ->
                input.seek(80)
                val count = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
                largeTriangleCount(count, file.length(), limit)
            }
        } catch (_: IOException) { null }
    }

    /** Pure policy check, also lets tests verify large STL thresholds without allocating huge files. */
    internal fun largeTriangleCount(triangleCount: Long, fileLength: Long, limit: Long = 500_000L): Long? =
        triangleCount.takeIf { it > limit && fileLength == 84L + 50L * it }

    /**
     * Estimates an STL's largest axis-aligned XY footprint after a possible 90-degree face-down
     * rotation. This is only an import ordering hint; the native orienter and packer remain the
     * authority for final placement. ASCII, malformed, and oversized files return null.
     */
    fun largestOrientedFootprintArea(file: File): Double? {
        if (!file.name.endsWith(".stl", ignoreCase = true)) return null
        return try {
            file.inputStream().buffered().use { largestOrientedFootprintArea(it, file.length()) }
        } catch (_: IOException) { null }
    }

    internal fun largestOrientedFootprintArea(input: InputStream, knownLength: Long = -1L): Double? {
        val data = DataInputStream(BufferedInputStream(input))
        val header = ByteArray(84)
        try { data.readFully(header) } catch (_: IOException) { return null }
        val count = littleEndianUInt(header, 80)
        if (count <= 0L || count > MAX_PREFLIGHT_FACETS) return null
        if (knownLength >= 0L && knownLength != 84L + count * 50L) return null

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        val facet = ByteArray(50)
        try {
            repeat(count.toInt()) {
                data.readFully(facet)
                for (vertex in 0 until 3) {
                    val offset = 12 + vertex * 12
                    val x = littleEndianFloat(facet, offset)
                    val y = littleEndianFloat(facet, offset + 4)
                    val z = littleEndianFloat(facet, offset + 8)
                    if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
                    minX = minOf(minX, x); maxX = maxOf(maxX, x)
                    minY = minOf(minY, y); maxY = maxOf(maxY, y)
                    minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
                }
            }
        } catch (_: IOException) { return null }
        val dx = (maxX - minX).toDouble()
        val dy = (maxY - minY).toDouble()
        val dz = (maxZ - minZ).toDouble()
        return maxOf(dx * dy, dx * dz, dy * dz).takeIf { it.isFinite() && it > 0.0 }
    }

    private fun littleEndianUInt(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xff) or
            ((bytes[offset + 1].toLong() and 0xff) shl 8) or
            ((bytes[offset + 2].toLong() and 0xff) shl 16) or
            ((bytes[offset + 3].toLong() and 0xff) shl 24)

    private fun littleEndianFloat(bytes: ByteArray, offset: Int): Float =
        java.lang.Float.intBitsToFloat(
            (bytes[offset].toInt() and 0xff) or
                ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                ((bytes[offset + 2].toInt() and 0xff) shl 16) or
                ((bytes[offset + 3].toInt() and 0xff) shl 24),
        )
}
