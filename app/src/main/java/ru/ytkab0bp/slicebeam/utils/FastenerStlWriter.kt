package ru.ytkab0bp.slicebeam.utils

import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Small parametric printable hardware parts. Dimensions are millimetres. */
object FastenerStlWriter {
    private const val SIDES = 96
    // Bound axial tessellation so unusually fine, long threads cannot exhaust the phone heap.
    private const val MAX_AXIAL_STEPS = 900

    /** Writes a hex-head bolt with a true helical triangular external thread. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeBolt(file: File, diameter: Float, pitch: Float, length: Float,
                  headAcrossFlats: Float, headHeight: Float): Int {
        requireDimension(diameter, 1f, 16f, "Schraubendurchmesser")
        requireDimension(pitch, .25f, 3f, "Gewindesteigung")
        requireDimension(length, 3f, 160f, "Schraubenlänge")
        requireDimension(headAcrossFlats, diameter * 1.4f, diameter * 3.5f, "Schlüsselweite")
        requireDimension(headHeight, diameter * .45f, diameter * 1.8f, "Kopfhöhe")
        val mesh = Mesh()
        val minor = diameter - 1.08f * pitch
        if (minor <= 0f) throw IOException("Gewindesteigung passt nicht zum Durchmesser.")
        val steps = axialSteps(length, pitch)
        val z0 = headHeight
        val rings = Array(steps + 1) { Array(SIDES) { FloatArray(3) } }
        for (z in 0..steps) {
            val axial = length * z / steps
            for (a in 0 until SIDES) {
                val angle = Math.PI * 2 * a / SIDES
                val phase = axial / pitch - angle / (Math.PI * 2)
                val f = phase - floor(phase)
                val ridge = if (f < .5) f * 2 else (1 - f) * 2
                // Trapezoidal approximation to a printable ISO-style external thread.
                val radius = minor / 2 + (diameter / 2 - minor / 2) * min(1.0, ridge * 1.35)
                rings[z][a][0] = (radius * cos(angle)).toFloat()
                rings[z][a][1] = (radius * sin(angle)).toFloat()
                rings[z][a][2] = z0 + axial
            }
        }
        for (z in 0 until steps) for (a in 0 until SIDES) {
            val b = (a + 1) % SIDES
            mesh.quad(rings[z][a], rings[z + 1][a], rings[z + 1][b], rings[z][b])
        }
        mesh.fan(rings[steps], floatArrayOf(0f, 0f, z0 + length), true)
        addHexHead(mesh, headAcrossFlats, headHeight, rings[0])
        mesh.write(file, "OrkanSlicer threaded bolt")
        return mesh.count
    }

    /** Hex nut with a validated clearance bore; the bore is intentionally unthreaded. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeNut(file: File, acrossFlats: Float, height: Float, boreDiameter: Float): Int {
        requireDimension(acrossFlats, 3f, 32f, "Schlüsselweite")
        requireDimension(height, 1.2f, 24f, "Mutternhöhe")
        requireDimension(boreDiameter, 1f, acrossFlats * .72f, "Durchgangsloch")
        if (boreDiameter > acrossFlats - 1.8f) {
            throw IOException("Das Loch ist zu groß; rundherum müssen mindestens 0,9 mm Wandstärke bleiben.")
        }
        val mesh = Mesh()
        addHexTube(mesh, acrossFlats, boreDiameter, height)
        mesh.write(file, "OrkanSlicer clearance-bore hex nut")
        return mesh.count
    }

    /** Writes a hex nut with a helical internal thread matched to writeBolt and radial fit clearance. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeThreadedNut(file: File, diameter: Float, pitch: Float, acrossFlats: Float,
                         height: Float, radialClearance: Float): Int {
        requireDimension(diameter, 1f, 16f, "Gewinde-Nenndurchmesser")
        requireDimension(pitch, .25f, 3f, "Gewindesteigung")
        requireDimension(acrossFlats, diameter * 1.4f, diameter * 3.5f, "Schlüsselweite")
        requireDimension(height, 1.2f, 24f, "Mutternhöhe")
        requireDimension(radialClearance, .05f, .7f, "Radiales Gewindespiel")
        val minorDiameter = diameter - 1.08f * pitch
        if (minorDiameter <= .4f) throw IOException("Gewindesteigung passt nicht zum Durchmesser.")
        if (acrossFlats - diameter - radialClearance * 2f < 1.3f) {
            throw IOException("Die Sechskantwand wäre zu dünn. Vergrößere die Schlüsselweite oder reduziere das Gewindespiel.")
        }
        val mesh = Mesh()
        addHexThreadedTube(mesh, acrossFlats, diameter, pitch, radialClearance, height)
        mesh.write(file, "OrkanSlicer helical threaded hex nut")
        return mesh.count
    }

    /** Writes one bolt and three nuts so users can compare common FDM thread clearances. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeThreadFitCoupon(file: File, diameter: Float, pitch: Float, length: Float,
                             acrossFlats: Float, headHeight: Float, nutHeight: Float): Int {
        val parent = file.absoluteFile.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Der Ausgabeordner für die Passprobe konnte nicht erstellt werden.")
        }
        val bolt = File.createTempFile("slicer-fit-bolt-", ".stl", parent)
        val nuts = arrayOfNulls<File>(3)
        try {
            writeBolt(bolt, diameter, pitch, length, acrossFlats, headHeight)
            for (i in nuts.indices) {
                nuts[i] = File.createTempFile("slicer-fit-nut-", ".stl", parent)
                writeThreadedNut(nuts[i]!!, diameter, pitch, acrossFlats, nutHeight,
                    FastenerStandards.threadClearance(i))
            }
            var total = facetCount(bolt)
            for (nut in nuts) total += facetCount(nut!!)
            if (total > 0xffffffffL) throw IOException("Die Passprobe wäre zu groß.")
            val radius = acrossFlats / sqrt(3f)
            val spacing = radius * 2f + 2f
            BufferedOutputStream(FileOutputStream(file)).use { output ->
                writeHeader(output, "OrkanSlicer thread fit coupon", total)
                appendTranslatedStl(bolt, output, 0f, 0f)
                appendTranslatedStl(nuts[0]!!, output, spacing, 0f)
                appendTranslatedStl(nuts[1]!!, output, 0f, spacing)
                appendTranslatedStl(nuts[2]!!, output, spacing, spacing)
            }
            return total.toInt()
        } finally {
            bolt.delete()
            for (nut in nuts) nut?.delete()
        }
    }

    private fun facetCount(file: File): Long = RandomAccessFile(file, "r").use { input ->
        if (input.length() < 84) throw IOException("Die STL-Passprobe ist unvollständig.")
        input.seek(80)
        val bytes = ByteArray(4)
        input.readFully(bytes)
        val count = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
        if (input.length() != 84L + count * 50L) throw IOException("Die STL-Passprobe hat eine ungültige Facettenanzahl.")
        count
    }

    private fun appendTranslatedStl(file: File, output: OutputStream, offsetX: Float, offsetY: Float) {
        RandomAccessFile(file, "r").use { input ->
            input.seek(84)
            val count = facetCount(file)
            val normal = ByteArray(12)
            val attribute = ByteArray(2)
            for (i in 0 until count) {
                input.readFully(normal)
                output.write(normal)
                for (vertex in 0..2) {
                    val x = readFloatLE(input) + offsetX
                    val y = readFloatLE(input) + offsetY
                    val z = readFloatLE(input)
                    writeFloatLE(output, x)
                    writeFloatLE(output, y)
                    writeFloatLE(output, z)
                }
                input.readFully(attribute)
                output.write(attribute)
            }
        }
    }

    private fun readFloatLE(input: RandomAccessFile): Float {
        val bytes = ByteArray(4)
        input.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
    }

    private fun writeHeader(output: OutputStream, label: String, facets: Long) {
        val header = ByteArray(80)
        val name = label.toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(name, 0, header, 0, min(name.size, header.size))
        output.write(header)
        writeIntLE(output, facets.toInt())
    }

    private fun writeIntLE(output: OutputStream, value: Int) {
        output.write(value)
        output.write(value ushr 8)
        output.write(value ushr 16)
        output.write(value ushr 24)
    }

    private fun writeFloatLE(output: OutputStream, value: Float) = writeIntLE(output, java.lang.Float.floatToIntBits(value))

    /** Flat washer with a through-hole and a configurable radial fit allowance. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeWasher(file: File, outerDiameter: Float, boreDiameter: Float, thickness: Float): Int {
        requireDimension(outerDiameter, 3f, 80f, "Scheiben-Außendurchmesser")
        requireDimension(boreDiameter, 1f, outerDiameter - 1f, "Scheibenloch")
        requireDimension(thickness, .6f, 12f, "Scheibendicke")
        if (boreDiameter >= outerDiameter - .8f) {
            throw IOException("Der Rand wäre zu schmal. Verkleinere das Loch oder vergrößere die Scheibe.")
        }
        val mesh = Mesh()
        addTube(mesh, outerDiameter / 2, boreDiameter / 2, thickness)
        mesh.write(file, "OrkanSlicer washer")
        return mesh.count
    }

    private fun addHexHead(mesh: Mesh, flats: Float, height: Float, shankRing: Array<FloatArray>) {
        val bottom = Array(SIDES) { FloatArray(3) }
        val top = Array(SIDES) { FloatArray(3) }
        for (i in 0 until SIDES) {
            val angle = Math.PI * 2 * i / SIDES
            val sector = angle % (Math.PI / 3) - Math.PI / 6
            val radius = (flats / 2) / cos(sector)
            bottom[i] = point(radius * cos(angle), radius * sin(angle), 0.0)
            top[i] = point(bottom[i][0].toDouble(), bottom[i][1].toDouble(), height.toDouble())
        }
        mesh.fan(bottom, floatArrayOf(0f, 0f, 0f), false)
        for (i in 0 until SIDES) {
            val next = (i + 1) % SIDES
            mesh.quad(bottom[i], bottom[next], top[next], top[i])
            // Join the head directly to the first shank ring, with no overlapping closed shells.
            mesh.quad(top[i], top[next], shankRing[next], shankRing[i])
        }
    }

    private fun addHexTube(mesh: Mesh, flats: Float, hole: Float, height: Float) {
        val outer = flats / sqrt(3f)
        val innerRadius = hole / 2
        val cap = Array(SIDES) { FloatArray(3) }
        val capTop = Array(SIDES) { FloatArray(3) }
        val innerBottom = Array(SIDES) { FloatArray(3) }
        val innerTop = Array(SIDES) { FloatArray(3) }
        for (i in 0 until SIDES) {
            val angle = Math.PI * 2 * i / SIDES
            val sector = (angle + Math.PI / 6) % (Math.PI / 3) - Math.PI / 6
            val radius = (flats / 2) / cos(sector)
            cap[i] = point(radius * cos(angle), radius * sin(angle), 0.0)
            capTop[i] = point(cap[i][0].toDouble(), cap[i][1].toDouble(), height.toDouble())
            innerBottom[i] = point(innerRadius * cos(angle), innerRadius * sin(angle), 0.0)
            innerTop[i] = point(innerBottom[i][0].toDouble(), innerBottom[i][1].toDouble(), height.toDouble())
        }
        // Sample the hex boundary so annular caps align with the circular bore.
        for (i in 0 until SIDES) {
            val next = (i + 1) % SIDES
            mesh.quad(cap[i], cap[next], capTop[next], capTop[i])
            mesh.quad(cap[i], innerBottom[i], innerBottom[next], cap[next])
            mesh.quad(capTop[i], capTop[next], innerTop[next], innerTop[i])
            mesh.quad(innerBottom[i], innerTop[i], innerTop[next], innerBottom[next])
        }
    }

    @Throws(IOException::class)
    private fun addHexThreadedTube(mesh: Mesh, flats: Float, diameter: Float, pitch: Float,
                                   clearance: Float, height: Float) {
        val minorRadius = (diameter - 1.08f * pitch) / 2f
        val steps = axialSteps(height, pitch)
        val cap = Array(SIDES) { FloatArray(3) }
        val capTop = Array(SIDES) { FloatArray(3) }
        val innerBottom = Array(SIDES) { FloatArray(3) }
        val innerTop = Array(SIDES) { FloatArray(3) }
        for (i in 0 until SIDES) {
            val angle = Math.PI * 2 * i / SIDES
            val sector = (angle + Math.PI / 6) % (Math.PI / 3) - Math.PI / 6
            val radius = (flats / 2) / cos(sector)
            cap[i] = point(radius * cos(angle), radius * sin(angle), 0.0)
            capTop[i] = point(cap[i][0].toDouble(), cap[i][1].toDouble(), height.toDouble())
            var radial = threadRadius(0f, angle, diameter, pitch, minorRadius, clearance)
            innerBottom[i] = point(radial * cos(angle), radial * sin(angle), 0.0)
            radial = threadRadius(height, angle, diameter, pitch, minorRadius, clearance)
            innerTop[i] = point(radial * cos(angle), radial * sin(angle), height.toDouble())
        }
        for (i in 0 until SIDES) {
            val next = (i + 1) % SIDES
            mesh.quad(cap[i], cap[next], capTop[next], capTop[i])
            mesh.quad(cap[i], innerBottom[i], innerBottom[next], cap[next])
            mesh.quad(capTop[i], capTop[next], innerTop[next], innerTop[i])
        }
        val rings = Array(steps + 1) { Array(SIDES) { FloatArray(3) } }
        for (z in 0..steps) {
            val axial = height * z / steps
            for (a in 0 until SIDES) {
                val angle = Math.PI * 2 * a / SIDES
                val radial = threadRadius(axial, angle, diameter, pitch, minorRadius, clearance)
                rings[z][a] = point(radial * cos(angle), radial * sin(angle), axial.toDouble())
            }
        }
        for (z in 0 until steps) for (a in 0 until SIDES) {
            val next = (a + 1) % SIDES
            // This is the inside boundary; its normal points into the threaded bore.
            mesh.quad(rings[z][a], rings[z + 1][a], rings[z + 1][next], rings[z][next])
        }
    }

    private fun threadRadius(z: Float, angle: Double, diameter: Float, pitch: Float,
                             minorRadius: Float, clearance: Float): Float {
        val phase = z / pitch - angle / (Math.PI * 2)
        val f = phase - floor(phase)
        val ridge = if (f < .5) f * 2 else (1 - f) * 2
        val radius = minorRadius + (diameter / 2f - minorRadius) * min(1.0, ridge * 1.35)
        return (radius + clearance).toFloat()
    }

    @Throws(IOException::class)
    private fun axialSteps(length: Float, pitch: Float): Int {
        val requested = ceil(length / pitch * 12.0)
        if (!requested.isFinite() || requested > MAX_AXIAL_STEPS) {
            throw IOException("Das Gewinde wäre für die mobile Erzeugung zu fein. Verkürze das Teil oder wähle eine größere Gewindesteigung.")
        }
        return max(24, requested.toInt())
    }

    private fun addTube(mesh: Mesh, outerRadius: Float, innerRadius: Float, height: Float) {
        for (i in 0 until SIDES) {
            val next = (i + 1) % SIDES
            val a = Math.PI * 2 * i / SIDES
            val b = Math.PI * 2 * next / SIDES
            val outerBottom = point(outerRadius * cos(a), outerRadius * sin(a), 0.0)
            val outerBottomNext = point(outerRadius * cos(b), outerRadius * sin(b), 0.0)
            val outerTop = point(outerBottom[0].toDouble(), outerBottom[1].toDouble(), height.toDouble())
            val outerTopNext = point(outerBottomNext[0].toDouble(), outerBottomNext[1].toDouble(), height.toDouble())
            val innerBottom = point(innerRadius * cos(a), innerRadius * sin(a), 0.0)
            val innerBottomNext = point(innerRadius * cos(b), innerRadius * sin(b), 0.0)
            val innerTop = point(innerBottom[0].toDouble(), innerBottom[1].toDouble(), height.toDouble())
            val innerTopNext = point(innerBottomNext[0].toDouble(), innerBottomNext[1].toDouble(), height.toDouble())
            mesh.quad(outerBottom, outerBottomNext, outerTopNext, outerTop)
            mesh.quad(innerBottom, innerTop, innerTopNext, innerBottomNext)
            mesh.quad(outerTop, outerTopNext, innerTopNext, innerTop)
            mesh.quad(outerBottom, innerBottom, innerBottomNext, outerBottomNext)
        }
    }

    private fun point(x: Double, y: Double, z: Double) = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())

    @Throws(IOException::class)
    private fun requireDimension(value: Float, min: Float, max: Float, label: String) {
        if (!value.isFinite() || value < min || value > max) {
            throw IOException("$label: zulässig sind $min bis $max mm.")
        }
    }

    private class Mesh {
        // Keep triangle coordinates in one primitive array. One FloatArray object per triangle
        // creates heavy GC pressure for threaded M12/M16 parts on mobile devices.
        private var coordinates = FloatArray(9 * 1024)
        var count = 0

        fun quad(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray) {
            tri(a, b, c)
            tri(a, c, d)
        }

        fun fan(ring: Array<FloatArray>, center: FloatArray, up: Boolean) {
            for (i in ring.indices) {
                val next = (i + 1) % ring.size
                if (up) tri(center, ring[i], ring[next]) else tri(center, ring[next], ring[i])
            }
        }

        private fun tri(a: FloatArray, b: FloatArray, c: FloatArray) {
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]
            val area2 = sqrt((uy * vz - uz * vy).toDouble().pow(2) +
                (uz * vx - ux * vz).toDouble().pow(2) + (ux * vy - uy * vx).toDouble().pow(2))
            if (area2 < 1e-7) return
            val required = (count + 1) * 9
            if (required > coordinates.size) {
                var capacity = coordinates.size
                while (capacity < required) capacity = (capacity * 2).coerceAtLeast(required)
                coordinates = coordinates.copyOf(capacity)
            }
            val offset = count * 9
            coordinates[offset] = a[0]; coordinates[offset + 1] = a[1]; coordinates[offset + 2] = a[2]
            coordinates[offset + 3] = b[0]; coordinates[offset + 4] = b[1]; coordinates[offset + 5] = b[2]
            coordinates[offset + 6] = c[0]; coordinates[offset + 7] = c[1]; coordinates[offset + 8] = c[2]
            count++
        }

        @Throws(IOException::class)
        fun write(file: File, headerLabel: String) {
            DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { output ->
                val header = ByteArray(80)
                val label = headerLabel.toByteArray(StandardCharsets.US_ASCII)
                System.arraycopy(label, 0, header, 0, min(label.size, header.size))
                output.write(header)
                writeInt(output, count)
                // Reuse one fixed-size STL record; per-coordinate ByteBuffer allocations made
                // large fasteners needlessly slow and memory intensive on Android.
                val record = ByteArray(50)
                val values = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                for (index in 0 until count) {
                    val offset = index * 9
                    val ux = coordinates[offset + 3] - coordinates[offset]
                    val uy = coordinates[offset + 4] - coordinates[offset + 1]
                    val uz = coordinates[offset + 5] - coordinates[offset + 2]
                    val vx = coordinates[offset + 6] - coordinates[offset]
                    val vy = coordinates[offset + 7] - coordinates[offset + 1]
                    val vz = coordinates[offset + 8] - coordinates[offset + 2]
                    val nx = uy * vz - uz * vy
                    val ny = uz * vx - ux * vz
                    val nz = ux * vy - uy * vx
                    val norm = sqrt(nx.toDouble() * nx + ny.toDouble() * ny + nz.toDouble() * nz)
                    values.clear()
                    values.putFloat((nx / norm).toFloat())
                    values.putFloat((ny / norm).toFloat())
                    values.putFloat((nz / norm).toFloat())
                    for (coordinate in offset until offset + 9) values.putFloat(coordinates[coordinate])
                    values.putShort(0)
                    output.write(record)
                }
            }
        }

        private fun writeInt(output: DataOutputStream, value: Int) {
            output.write(value)
            output.write(value ushr 8)
            output.write(value ushr 16)
            output.write(value ushr 24)
        }
    }
}
