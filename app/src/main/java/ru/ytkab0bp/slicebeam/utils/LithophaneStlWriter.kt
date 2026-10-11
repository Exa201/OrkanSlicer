package ru.ytkab0bp.slicebeam.utils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.nio.ByteOrder
/** Writes watertight relief panels and a printable four-photo lithophane cube kit as binary STL. */
object LithophaneStlWriter {
@Throws(IOException::class)
@JvmStatic
fun writePanel(file:File, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float) {
validateGrid(topZ, cols, rows, width, height)
val facets = panelFacetCount(cols, rows)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer Lithophane panel", facets)
writePanelMesh(out, topZ, cols, rows, width, height, 0f, 0f) })
}
/** Writes a backlit photo panel following a cylindrical curve, closed at every edge. */
 @Throws(IOException::class)
@JvmStatic
fun writeCurvedPanel(file:File, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float, radius:Float) {
validateGrid(topZ, cols, rows, width, height)
if (!java.lang.Float.isFinite(radius) || radius < width / (Math.PI).toFloat())
    throw IOException("Der Radius ist fÃ¼r die gewÃ¤hlte Bildbreite zu klein.")
val facets = 4L * (cols - 1L) * (rows - 1L) + 4L * (cols - 1L) + 4L * (rows - 1L)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer curved Lithophane panel", facets)
val maxAngle = width / (2f * radius)
val zOffset = radius * (Math.cos((maxAngle).toDouble())).toFloat()
for (y in 0 until rows - 1)
    for (x in 0 until cols - 1)
{
val t00 = topZ[y * cols + x]
val t10 = topZ[y * cols + x + 1]
val t01 = topZ[(y + 1) * cols + x]
val t11 = topZ[(y + 1) * cols + x + 1]
val o00 = curvedPoint(x, y, cols, rows, width, height, radius + t00, maxAngle, zOffset)
val o10 = curvedPoint(x + 1, y, cols, rows, width, height, radius + t10, maxAngle, zOffset)
val o01 = curvedPoint(x, y + 1, cols, rows, width, height, radius + t01, maxAngle, zOffset)
val o11 = curvedPoint(x + 1, y + 1, cols, rows, width, height, radius + t11, maxAngle, zOffset)
val i00 = curvedPoint(x, y, cols, rows, width, height, radius, maxAngle, zOffset)
val i10 = curvedPoint(x + 1, y, cols, rows, width, height, radius, maxAngle, zOffset)
val i01 = curvedPoint(x, y + 1, cols, rows, width, height, radius, maxAngle, zOffset)
val i11 = curvedPoint(x + 1, y + 1, cols, rows, width, height, radius, maxAngle, zOffset)
facet(out, o00[0], o00[1], o00[2], o10[0], o10[1], o10[2], o11[0], o11[1], o11[2])
facet(out, o00[0], o00[1], o00[2], o11[0], o11[1], o11[2], o01[0], o01[1], o01[2])
facet(out, i00[0], i00[1], i00[2], i11[0], i11[1], i11[2], i10[0], i10[1], i10[2])
facet(out, i00[0], i00[1], i00[2], i01[0], i01[1], i01[2], i11[0], i11[1], i11[2])
}
for (x in 0 until cols - 1)
{
writeCurvedEdge(out, x, 0, x + 1, 0, topZ[x], topZ[x + 1], cols, rows, width, height, radius, maxAngle, zOffset)
val a = (rows - 1) * cols + x
val b = a + 1
writeCurvedEdge(out, x + 1, rows - 1, x, rows - 1, topZ[b], topZ[a], cols, rows, width, height, radius, maxAngle, zOffset)
}
for (y in 0 until rows - 1)
{
var a = y * cols + cols - 1
var b = (y + 1) * cols + cols - 1
writeCurvedEdge(out, cols - 1, y, cols - 1, y + 1, topZ[a], topZ[b], cols, rows, width, height, radius, maxAngle, zOffset)
a = (y + 1) * cols
b = y * cols
writeCurvedEdge(out, 0, y + 1, 0, y, topZ[a], topZ[b], cols, rows, width, height, radius, maxAngle, zOffset)
} })
}
/** Writes an enclosed cylindrical/spherical/vase/pumpkin lithophane shell from a latitude-longitude image grid. */
 @Throws(IOException::class)
@JvmStatic
fun writeRotationalLithophane(file:File, topZ:FloatArray, cols:Int, rows:Int,
diameter:Float, height:Float, shape:Int,
topDiameter:Float, waveAmplitude:Float, waveCount:Int,
closeBottom:Boolean, closeTop:Boolean) {
validateGrid(topZ, cols, rows, diameter, height)
if ((shape < 0 || shape > 4 || !java.lang.Float.isFinite(topDiameter) || topDiameter < diameter * 0.2f ||
topDiameter > diameter * 1.8f || waveAmplitude < 0f || waveAmplitude > diameter * 0.12f || waveCount < 0 || waveCount > 24))
    throw IOException("Die Formparameter fÃ¼r das Rundmodell sind ungÃ¼ltig.")
val around = cols
val vertical = rows
if (around < 8 || vertical < 4) throw IOException("FÃ¼r runde Lithophane ist eine hÃ¶here AuflÃ¶sung erforderlich.")
val facets = (4L * around.toLong() * (vertical - 1L)
+ (if (closeBottom) 2L * around else 0L) + (if (closeTop) 2L * around else 0L))
if (facets > 0xffffffffL) throw IOException("Das Rundmodell ist zu groÃŸ.")
val baseRadius = diameter / 2f
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer rotational lithophane", facets)
for (y in 0 until vertical - 1)
    for (x in 0 until around)
{
val xn = (x + 1) % around
val a = rotationalPoint(x, y, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[y * cols + x])
val b = rotationalPoint(xn, y, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[y * cols + xn])
val c = rotationalPoint(xn, y + 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[(y + 1) * cols + xn])
val d = rotationalPoint(x, y + 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[(y + 1) * cols + x])
val ia = rotationalPoint(x, y, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
val ib = rotationalPoint(xn, y, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
val ic = rotationalPoint(xn, y + 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
val id = rotationalPoint(x, y + 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
facet(out, a[0], a[1], a[2], c[0], c[1], c[2], b[0], b[1], b[2])
facet(out, a[0], a[1], a[2], d[0], d[1], d[2], c[0], c[1], c[2])
facet(out, ia[0], ia[1], ia[2], ib[0], ib[1], ib[2], ic[0], ic[1], ic[2])
facet(out, ia[0], ia[1], ia[2], ic[0], ic[1], ic[2], id[0], id[1], id[2])
}
if (closeBottom)
    for (x in 0 until around)
{
val xn = (x + 1) % around
val a = rotationalPoint(x, 0, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[x])
val b = rotationalPoint(xn, 0, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[xn])
val ia = rotationalPoint(x, 0, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
val ib = rotationalPoint(xn, 0, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
facet(out, a[0], a[1], a[2], b[0], b[1], b[2], ib[0], ib[1], ib[2])
facet(out, a[0], a[1], a[2], ib[0], ib[1], ib[2], ia[0], ia[1], ia[2])
}
if (closeTop)
{
for (x in 0 until around)
{
val xn = (x + 1) % around
val a = rotationalPoint(x, vertical - 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[(vertical - 1) * cols + x])
val b = rotationalPoint(xn, vertical - 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, topZ[(vertical - 1) * cols + xn])
val ia = rotationalPoint(x, vertical - 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
val ib = rotationalPoint(xn, vertical - 1, around, vertical, baseRadius, topDiameter / 2f, height, shape, waveAmplitude, waveCount, 0f)
facet(out, a[0], a[1], a[2], b[0], b[1], b[2], ib[0], ib[1], ib[2])
facet(out, a[0], a[1], a[2], ib[0], ib[1], ib[2], ia[0], ia[1], ia[2])
}
} })
}
/** Writes a simple accessory base with a centered light opening and cable pass-through. */
 @Throws(IOException::class)
@JvmStatic
fun writeRoundLightBase(file:File, outerDiameter:Float, innerDiameter:Float,
thickness:Float, cableDiameter:Float, segments:Int) {
if ((!java.lang.Float.isFinite(outerDiameter) || !java.lang.Float.isFinite(innerDiameter) || !java.lang.Float.isFinite(thickness) ||
!java.lang.Float.isFinite(cableDiameter) || outerDiameter < 25f || innerDiameter < 5f ||
innerDiameter >= outerDiameter - 4f || thickness < 2f || thickness > 20f ||
cableDiameter < 0f || cableDiameter >= innerDiameter * 0.7f || segments < 24 || segments > 192))
    throw IOException("Die MaÃŸe der Lampenbasis sind ungÃ¼ltig.")
val facets = 8L * segments
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer lithophane light base", facets)
ringMesh(out, outerDiameter / 2f, if (cableDiameter > 0f) cableDiameter / 2f else innerDiameter / 2f,
0f, thickness, segments, true) })
}
@Throws(IOException::class)
private fun ringMesh(out:OutputStream, outer:Float, inner:Float, z0:Float, z1:Float,
segments:Int, capInner:Boolean) {
for (i in 0 until segments)
{
val n = (i + 1) % segments
val a = 2.0 * Math.PI * i / segments
val b = 2.0 * Math.PI * n / segments
val ox0 = outer * (Math.cos(a)).toFloat()
val oy0 = outer * (Math.sin(a)).toFloat()
val ox1 = outer * (Math.cos(b)).toFloat()
val oy1 = outer * (Math.sin(b)).toFloat()
val ix0 = inner * (Math.cos(a)).toFloat()
val iy0 = inner * (Math.sin(a)).toFloat()
val ix1 = inner * (Math.cos(b)).toFloat()
val iy1 = inner * (Math.sin(b)).toFloat()
quad(out, ox0, oy0, z0, ox1, oy1, z0, ox1, oy1, z1, ox0, oy0, z1, true)
quad(out, ix0, iy0, z0, ix1, iy1, z0, ix1, iy1, z1, ix0, iy0, z1, false)
quad(out, ox0, oy0, z1, ox1, oy1, z1, ix1, iy1, z1, ix0, iy0, z1, true)
quad(out, ox1, oy1, z0, ox0, oy0, z0, ix0, iy0, z0, ix1, iy1, z0, true)
}
}
private fun rotationalPoint(x:Int, y:Int, cols:Int, rows:Int, bottomRadius:Float,
topRadius:Float, height:Float,
shape:Int, waveAmplitude:Float, waveCount:Int, relief:Float):FloatArray {
val v = y / (rows - 1).toFloat()
val theta = x / cols.toFloat() * ((Math.PI * 2.0)).toFloat()
val radius = bottomRadius + (topRadius - bottomRadius) * v
val profile:Float
if (shape == 1)
profile = 0.12f + 0.88f * (Math.sin(Math.PI * Math.pow((v).toDouble(), 0.82))).toFloat() // vase
else if (shape == 2)
profile = 0.82f + 0.18f * (Math.sin(Math.PI * v)).toFloat() // bell
else if (shape == 3)
profile = 0.12f + 0.88f * (Math.sin(Math.PI * v)).toFloat() // pumpkin
else if (shape == 4)
profile = 0.55f + 0.45f * (Math.sin(Math.PI * v)).toFloat() // lamp shade
else
profile = Math.max(0.035f, (Math.sqrt(Math.max(0.0, 1.0 - Math.pow(2.0 * v - 1.0, 2.0)))).toFloat()) // sphere
val grooves = if (shape == 3 && waveCount > 0) 1f + 0.1f * (Math.cos((waveCount * theta).toDouble())).toFloat() else 1f
val wave = if (waveCount > 0 && shape != 3) waveAmplitude * (Math.sin((waveCount * theta).toDouble())).toFloat() else 0f
val r = Math.max(0.5f, radius * profile * grooves + wave + relief)
return floatArrayOf(r * (Math.cos((theta).toDouble())).toFloat(), v * height, r * (Math.sin((theta).toDouble())).toFloat())
}
@Throws(IOException::class)
private fun writeCurvedEdge(out:OutputStream, ax:Int, ay:Int, bx:Int, by:Int,
ta:Float, tb:Float, cols:Int, rows:Int, width:Float, height:Float,
radius:Float, maxAngle:Float, zOffset:Float) {
val oa = curvedPoint(ax, ay, cols, rows, width, height, radius + ta, maxAngle, zOffset)
val ob = curvedPoint(bx, by, cols, rows, width, height, radius + tb, maxAngle, zOffset)
val ia = curvedPoint(ax, ay, cols, rows, width, height, radius, maxAngle, zOffset)
val ib = curvedPoint(bx, by, cols, rows, width, height, radius, maxAngle, zOffset)
facet(out, oa[0], oa[1], oa[2], ib[0], ib[1], ib[2], ob[0], ob[1], ob[2])
facet(out, oa[0], oa[1], oa[2], ia[0], ia[1], ia[2], ib[0], ib[1], ib[2])
}
private fun curvedPoint(x:Int, y:Int, cols:Int, rows:Int, width:Float, height:Float,
radial:Float, maxAngle:Float, zOffset:Float):FloatArray {
val angle = (x / (cols - 1).toFloat() - 0.5f) * 2f * maxAngle
return floatArrayOf(radial * (Math.sin((angle).toDouble())).toFloat(), y * height / (rows - 1), radial * (Math.cos((angle).toDouble())).toFloat() - zOffset)
}
/** Writes a raised-relief tag with an optional through-hole near its top edge. */
 @Throws(IOException::class)
@JvmStatic
fun writeTextTag(file:File, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float, holeDiameter:Float) {
validateGrid(topZ, cols, rows, width, height)
if (!java.lang.Float.isFinite(holeDiameter) || holeDiameter < 0f || holeDiameter >= Math.min(width, height) * 0.8f)
    throw IOException("Der Lochdurchmesser ist fÃ¼r dieses Schild zu groÃŸ.")
val hole = Array<BooleanArray>(rows - 1, {BooleanArray(cols - 1)})
val centerX = width - holeDiameter / 2f - Math.max(2f, holeDiameter * 0.25f)
val centerY = height / 2f
val radius = holeDiameter / 2f
var blocked = 0
if (holeDiameter > 0f)
{
for (y in 0 until rows - 1)
    for (x in 0 until cols - 1)
{
val px = (x + 0.5f) * width / (cols - 1)
val py = (y + 0.5f) * height / (rows - 1)
hole[y][x] = (px - centerX) * (px - centerX) + (py - centerY) * (py - centerY) < radius * radius
if (hole[y][x]) blocked++
}
if (blocked == 0) throw IOException("Das Schildraster ist fÃ¼r die gewÃ¤hlte LochgrÃ¶ÃŸe zu grob.")
}
var facets = panelFacetCount(cols, rows) - 4L * blocked
for (y in 0 until rows - 1)
    for (x in 0 until cols - 1)
if (!hole[y][x])
{
if (x > 0 && hole[y][x - 1]) facets += 2
if (x + 1 < cols - 1 && hole[y][x + 1]) facets += 2
if (y > 0 && hole[y - 1][x]) facets += 2
if (y + 1 < rows - 1 && hole[y + 1][x]) facets += 2
}
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer raised text tag", facets)
for (y in 0 until rows - 1)
    for (x in 0 until cols - 1)
if (!hole[y][x])
{
val x0 = x * width / (cols - 1)
val x1 = (x + 1) * width / (cols - 1)
val y0 = y * height / (rows - 1)
val y1 = (y + 1) * height / (rows - 1)
val z00 = topZ[y * cols + x]
val z10 = topZ[y * cols + x + 1]
val z01 = topZ[(y + 1) * cols + x]
val z11 = topZ[(y + 1) * cols + x + 1]
facet(out, x0, y0, z00, x1, y0, z10, x1, y1, z11)
facet(out, x0, y0, z00, x1, y1, z11, x0, y1, z01)
facet(out, x0, y0, 0f, x1, y1, 0f, x1, y0, 0f)
facet(out, x0, y0, 0f, x0, y1, 0f, x1, y1, 0f)
if (x > 0 && hole[y][x - 1]) wall(out, x0, y1, x0, y0, topZ[y * cols + x], topZ[(y + 1) * cols + x])
if (x + 1 < cols - 1 && hole[y][x + 1]) wall(out, x1, y0, x1, y1, topZ[y * cols + x + 1], topZ[(y + 1) * cols + x + 1])
if (y > 0 && hole[y - 1][x]) wall(out, x0, y0, x1, y0, topZ[y * cols + x], topZ[y * cols + x + 1])
if (y + 1 < rows - 1 && hole[y + 1][x]) wall(out, x1, y1, x0, y1, topZ[(y + 1) * cols + x + 1], topZ[(y + 1) * cols + x])
}
writeOuterWalls(out, topZ, cols, rows, width, height, 0f, 0f) })
}
/** Writes up to eight separate text tags arranged as a compact two-column STL kit. */
 @Throws(IOException::class)
@JvmStatic
fun writeTextTagBatch(file:File, topZList:List<FloatArray>, cols:Int, rows:Int,
width:Float, height:Float, holeDiameter:Float, gap:Float):Int {
if ((topZList == null || topZList.isEmpty() || topZList.size > 8 ||
!java.lang.Float.isFinite(gap) || gap < 0f || gap > 20f))
    throw IOException("FÃ¼r den Schilder-Satz sind 1 bis 8 Schilder und ein gÃ¼ltiger Abstand nÃ¶tig.")
val parent = file.getAbsoluteFile().getParentFile()
val parts = arrayOfNulls<File>(topZList.size)
var totalFacets = 0
try
{
for (i in 0 until topZList.size)
{
parts[i] = File.createTempFile("orca-plant-tag-", ".stl", parent)
writeTextTag(parts[i]!!, topZList.get(i), cols, rows, width, height, holeDiameter)
FileInputStream(parts[i]!!).use({ `in`->
skipFully(`in`, 80)
val facets = readIntLE(`in`)
if (facets < 1 || totalFacets > 0xffffffffL - facets)
    throw IOException("Der Schilder-Satz ist zu groÃŸ fÃ¼r eine STL-Datei.")
totalFacets += facets })
}
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer plant label set", totalFacets.toLong())
val facet = ByteArray(50)
for (i in parts.indices)
{
val dx = (i % 2) * (width + gap)
val dy = (i / 2) * (height + gap)
FileInputStream(parts[i]!!).use({ `in`->
skipFully(`in`, 84)
val facets = readIntLEAtHeader(parts[i]!!)
for (f in 0 until facets)
{
readFully(`in`, facet)
val data = ByteBuffer.wrap(facet).order(ByteOrder.LITTLE_ENDIAN)
for (vertex in 0..2)
{
val offset = 12 + vertex * 12
data.putFloat(offset, data.getFloat(offset) + dx)
data.putFloat(offset + 4, data.getFloat(offset + 4) + dy)
}
out.write(facet)
} })
} })
return totalFacets
}
finally
{
for (part in parts) if (part != null && part.exists()) part.delete()
}
}
@Throws(IOException::class)
private fun readIntLEAtHeader(file:File):Int {
FileInputStream(file).use({ `in`->
skipFully(`in`, 80)
return readIntLE(`in`) })
}
@Throws(IOException::class)
private fun readFully(`in`:FileInputStream, data:ByteArray) {
var position = 0
while (position < data.size)
{
val read = `in`.read(data, position, data.size - position)
if (read < 0) throw IOException("Ein erzeugtes Schild-STL ist unvollstÃ¤ndig.")
position += read
}
}
@Throws(IOException::class)
private fun readIntLE(`in`:FileInputStream):Int {
val b0 = `in`.read()
val b1 = `in`.read()
val b2 = `in`.read()
val b3 = `in`.read()
if ((b0 or b1 or b2 or b3) < 0) throw IOException("Die STL-Datei ist unvollstÃ¤ndig.")
return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}
@Throws(IOException::class)
private fun skipFully(`in`:FileInputStream, bytes:Long) {
var remaining = bytes
while (remaining > 0)
{
var skipped = `in`.skip(remaining)
if (skipped <= 0)
{
if (`in`.read() < 0) throw IOException("Die STL-Datei ist unvollstÃ¤ndig.")
skipped = 1
}
remaining -= skipped
}
}
/** Writes a hollow open-top box and optional matching slip-over lid as separate STL shells. */
 @Throws(IOException::class)
@JvmStatic
fun writeUtilityBox(file:File, width:Float, depth:Float, height:Float,
wall:Float, floor:Float, lidClearance:Float,
lidSkirt:Float, cableHole:Float, includeLid:Boolean) {
if ((!java.lang.Float.isFinite(width) || !java.lang.Float.isFinite(depth) || !java.lang.Float.isFinite(height) ||
!java.lang.Float.isFinite(wall) || !java.lang.Float.isFinite(floor) || !java.lang.Float.isFinite(lidClearance) ||
!java.lang.Float.isFinite(lidSkirt) || !java.lang.Float.isFinite(cableHole) ||
width <= wall * 2f + 4f || depth <= wall * 2f + 4f ||
height <= floor + 4f || wall < 1f || floor < 1f || lidClearance < 0f ||
lidSkirt < 3f || cableHole < 0f || cableHole >= Math.min(width - 2f * wall, depth - 2f * wall) * 0.65f))
    throw IOException("Die Box-MaÃŸe oder WandstÃ¤rken sind ungÃ¼ltig.")
val lidInnerWidth = width + 2f * lidClearance
val lidInnerDepth = depth + 2f * lidClearance
val lidOuterWidth = lidInnerWidth + 2f * wall
val lidOuterDepth = lidInnerDepth + 2f * wall
var lidOffsetX = 0f
var lidOffsetY = 0f
if (includeLid)
{
val sideX = width + 6f + lidOuterWidth
val sideY = Math.max(depth, lidOuterDepth)
val stackX = Math.max(width, lidOuterWidth)
val stackY = depth + 6f + lidOuterDepth
val sideBySide = (Math.max(sideX, sideY) <= 220f || (Math.max(stackX, stackY) > 220f && Math.max(sideX, sideY) <= Math.max(stackX, stackY)))
if (sideBySide)
lidOffsetX = width + 6f
else
    lidOffsetY = depth + 6f
if (Math.max(if (sideBySide) sideX else stackX, if (sideBySide) sideY else stackY) > 400f)
    throw IOException("Box und Deckel ergeben eine sehr groÃŸe DruckflÃ¤che. Verkleinere die Box oder wÃ¤hle â€žohne Deckelâ€œ.")
}
val baseFacets = if (cableHole > 0f) 6L * 32L + 8L else 12L
val facets = baseFacets + 24L + (if (includeLid) 28L else 0L)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer hollow utility box", facets)
if (cableHole > 0f)
writeFootRing(out, width, depth, cableHole, floor)
else
    writeBox(out, 0f, 0f, 0f, width, depth, floor)
writeBoxWalls(out, width, depth, wall, floor, height)
writeBoxRim(out, 0f, 0f, width, depth, wall, height, true)
if (includeLid)
writeLid(out, lidOffsetX, lidOffsetY, lidOuterWidth, lidOuterDepth,
wall, lidSkirt, 2.4f) })
}
/** Writes an open-ended snap clip and, optionally, a flat screw-mount tab. */
 @Throws(IOException::class)
@JvmStatic
fun writeCableClip(file:File, cableDiameter:Float, clearance:Float,
wall:Float, clipWidth:Float, mouthGap:Float,
mountTab:Boolean, tabLength:Float, screwHole:Float) {
if ((!java.lang.Float.isFinite(cableDiameter) || !java.lang.Float.isFinite(clearance) || !java.lang.Float.isFinite(wall) ||
!java.lang.Float.isFinite(clipWidth) || !java.lang.Float.isFinite(mouthGap) || !java.lang.Float.isFinite(tabLength) ||
!java.lang.Float.isFinite(screwHole) || cableDiameter < 2f || clearance < 0f || clearance > 1.5f ||
wall < 1.2f || wall > 6f || clipWidth < 4f || clipWidth > 40f ||
mouthGap < cableDiameter * 0.4f || mouthGap >= cableDiameter ||
(mountTab && (tabLength < 8f || tabLength > 40f || screwHole < 0f || screwHole >= 10f)) ||
(!mountTab && screwHole != 0f)))
    throw IOException("Kabeldurchmesser, Clipspalt oder WandstÃ¤rke sind ungÃ¼ltig.")
val innerRadius = cableDiameter / 2f + clearance
val outerRadius = innerRadius + wall
val angle = ((2.0 * Math.asin(mouthGap / (2.0 * outerRadius)))).toFloat()
if (!java.lang.Float.isFinite(angle) || angle <= 0f || angle >= Math.PI)
    throw IOException("Der Clipspalt lÃ¤sst sich mit diesen MaÃŸen nicht erzeugen.")
val segments = 72
val start = ((Math.PI / 2.0 + angle / 2.0)).toFloat()
val span = ((Math.PI * 2.0 - angle)).toFloat()
var facets = 8L * segments + 4L
val mountWidth = Math.max(clipWidth + 4f, cableDiameter + 2f * wall + 6f)
val mountTop = -outerRadius + wall * 0.55f
if (mountTab) facets += if (screwHole > 0f) 6L * 32L + 8L else 12L
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer C cable snap clip", facets)
val outerX = FloatArray(segments + 1)
val outerY = FloatArray(segments + 1)
val innerX = FloatArray(segments + 1)
val innerY = FloatArray(segments + 1)
for (i in 0..segments)
{
val a = (start + span * i / segments).toDouble()
val c = (Math.cos(a)).toFloat()
val s = (Math.sin(a)).toFloat()
outerX[i] = c * outerRadius
outerY[i] = s * outerRadius
innerX[i] = c * innerRadius
innerY[i] = s * innerRadius
}
for (i in 0 until segments)
{
val ox0 = outerX[i]
val oy0 = outerY[i]
val ox1 = outerX[i + 1]
val oy1 = outerY[i + 1]
val ix0 = innerX[i]
val iy0 = innerY[i]
val ix1 = innerX[i + 1]
val iy1 = innerY[i + 1]
quad(out, ox0, oy0, 0f, ox1, oy1, 0f, ox1, oy1, clipWidth, ox0, oy0, clipWidth, true)
quad(out, ix0, iy0, 0f, ix1, iy1, 0f, ix1, iy1, clipWidth, ix0, iy0, clipWidth, false)
quad(out, ox0, oy0, clipWidth, ox1, oy1, clipWidth, ix1, iy1, clipWidth, ix0, iy0, clipWidth, true)
quad(out, ox0, oy0, 0f, ox1, oy1, 0f, ix1, iy1, 0f, ix0, iy0, 0f, false)
}
quad(out, innerX[0], innerY[0], 0f, outerX[0], outerY[0], 0f,
outerX[0], outerY[0], clipWidth, innerX[0], innerY[0], clipWidth, true)
val end = segments
quad(out, outerX[end], outerY[end], 0f, innerX[end], innerY[end], 0f,
innerX[end], innerY[end], clipWidth, outerX[end], outerY[end], clipWidth, true)
if (mountTab)
{
val x = -mountWidth / 2f
val y = mountTop - tabLength
if (screwHole > 0f)
writeFootRingAt(out, x, y, mountWidth, tabLength, screwHole, clipWidth)
else
    writeBoxAt(out, x, y, 0f, x + mountWidth, mountTop, clipWidth)
} })
}
/** Writes a desk cable grommet with a square mounting flange and two screw ears. */
 @Throws(IOException::class)
@JvmStatic
fun writeCableGrommet(file:File, cableDiameter:Float, clearance:Float,
wall:Float, flangeSize:Float, flangeThickness:Float,
tubeLength:Float, screwHole:Float) {
if ((!java.lang.Float.isFinite(cableDiameter) || !java.lang.Float.isFinite(clearance) || !java.lang.Float.isFinite(wall) ||
!java.lang.Float.isFinite(flangeSize) || !java.lang.Float.isFinite(flangeThickness) ||
!java.lang.Float.isFinite(tubeLength) || !java.lang.Float.isFinite(screwHole) ||
cableDiameter < 2f || cableDiameter > 40f || clearance < 0f || clearance > 1.5f ||
wall < 1.2f || wall > 6f || flangeSize < 30f || flangeSize > 100f ||
flangeThickness < 1.5f || flangeThickness > 6f || tubeLength < 8f || tubeLength > 60f ||
screwHole < 2.5f || screwHole > 8f))
    throw IOException("Kabeldurchlass-MaÃŸe auÃŸerhalb des zulÃ¤ssigen Bereichs.")
val innerRadius = cableDiameter / 2f + clearance
val outerRadius = innerRadius + wall
if (flangeSize <= outerRadius * 2f + 10f || screwHole + 4f >= 16f)
    throw IOException("Flansch oder SchraubenlÃ¶cher sind fÃ¼r den Kabeldurchlass zu klein.")
val segments = 48
val earWidth = 16f
val earDepth = 20f
val earInset = 4f
val earX = flangeSize / 2f - earInset
val earY = -earDepth / 2f
val facets = 6L * 32L + 8L + 6L * segments + 2L * (6L * 32L + 8L)
val topZ = flangeThickness + tubeLength
if (topZ > 100f) throw IOException("Der Kabeldurchlass ist zu hoch fÃ¼r den Generator.")
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer flanged cable grommet", facets)
// Rectangular flange ring leaves the cable bore open through the desk.
 writeFootRingAt(out, -flangeSize / 2f, -flangeSize / 2f, flangeSize, flangeSize,
cableDiameter + 2f * clearance, flangeThickness)
// Screw ears overlap the flange by a small amount to form a connected part.
 writeFootRingAt(out, -earX - earWidth, earY, earWidth, earDepth,
screwHole, flangeThickness)
writeFootRingAt(out, earX, earY, earWidth, earDepth, screwHole, flangeThickness)
val ix = FloatArray(segments)
val iy = FloatArray(segments)
val ox = FloatArray(segments)
val oy = FloatArray(segments)
for (i in 0 until segments)
{
val a = Math.PI * 2.0 * i / segments
val c = (Math.cos(a)).toFloat()
val s = (Math.sin(a)).toFloat()
ix[i] = c * innerRadius
iy[i] = s * innerRadius
ox[i] = c * outerRadius
oy[i] = s * outerRadius
}
for (i in 0 until segments)
{
val n = (i + 1) % segments
quad(out, ox[i], oy[i], flangeThickness, ox[n], oy[n], flangeThickness,
ox[n], oy[n], topZ, ox[i], oy[i], topZ, true)
quad(out, ix[i], iy[i], flangeThickness, ix[n], iy[n], flangeThickness,
ix[n], iy[n], topZ, ix[i], iy[i], topZ, false)
quad(out, ox[i], oy[i], topZ, ox[n], oy[n], topZ,
ix[n], iy[n], topZ, ix[i], iy[i], topZ, true)
} })
}
@Throws(IOException::class)
@JvmStatic
@JvmOverloads fun writeWallToolHolder(file:File, toolDiameter:Float, clearance:Float,
holderCount:Int, plateThickness:Float,
screwHole:Float, guideHeight:Float = Math.max(10f, toolDiameter * 0.8f)) {
if ((!java.lang.Float.isFinite(toolDiameter) || !java.lang.Float.isFinite(clearance) ||
!java.lang.Float.isFinite(plateThickness) || !java.lang.Float.isFinite(screwHole) || !java.lang.Float.isFinite(guideHeight) ||
toolDiameter < 4f || toolDiameter > 30f || clearance < 0f || clearance > 1f ||
holderCount < 2 || holderCount > 6 || plateThickness < 2f || plateThickness > 6f ||
screwHole < 3f || screwHole > 8f || guideHeight < 6f || guideHeight > 35f))
    throw IOException("Werkzeugdurchmesser, Fachzahl oder PlattenmaÃŸe sind ungÃ¼ltig.")
val opening = toolDiameter + 2f * clearance
val cell = opening + 12f
val depth = opening + 12f
val rowWidth = holderCount * cell
val leftEarX = -rowWidth / 2f - 12f
val rightEarX = rowWidth / 2f - 4f
val earY = -10f
val earWidth = 16f
val earDepth = 20f
val facets = (holderCount * 2L + 2L) * (6L * 32L + 8L)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer wall tool holder", facets)
for (i in 0 until holderCount)
{
val x = -rowWidth / 2f + i * cell
writeFootRingAt(out, x, -depth / 2f, cell, depth, opening, plateThickness)
writeFootRingAt(out, x, -depth / 2f, cell, depth, opening,
plateThickness, plateThickness + guideHeight)
}
// Each ear overlaps the outer holder by 4 mm; the screw opening stays outside it.
 writeFootRingAt(out, leftEarX, earY, earWidth, earDepth, screwHole, plateThickness)
writeFootRingAt(out, rightEarX, earY, earWidth, earDepth, screwHole, plateThickness) })
}
/** Writes a tapered hollow planter and an optional matching saucer beside it. */
 @Throws(IOException::class)
@JvmStatic
fun writePlanterPot(file:File, bottomDiameter:Float, topDiameter:Float,
height:Float, wall:Float, bottomThickness:Float,
drainDiameter:Float, includeSaucer:Boolean,
saucerClearance:Float, saucerWall:Float,
saucerHeight:Float, saucerBase:Float) {
if ((!java.lang.Float.isFinite(bottomDiameter) || !java.lang.Float.isFinite(topDiameter) ||
!java.lang.Float.isFinite(height) || !java.lang.Float.isFinite(wall) ||
!java.lang.Float.isFinite(bottomThickness) || !java.lang.Float.isFinite(drainDiameter) ||
bottomDiameter < 30f || bottomDiameter > 180f || topDiameter < 30f || topDiameter > 200f ||
height < 20f || height > 220f || wall < 1.2f || wall > 5f ||
bottomThickness < 1.5f || bottomThickness > 8f || drainDiameter < 2f ||
drainDiameter > 20f || drainDiameter >= bottomDiameter - 2f * wall ||
((includeSaucer && ((!java.lang.Float.isFinite(saucerClearance) || !java.lang.Float.isFinite(saucerWall) ||
!java.lang.Float.isFinite(saucerHeight) || !java.lang.Float.isFinite(saucerBase) ||
saucerClearance < 1f || saucerClearance > 8f ||
saucerWall < 1.2f || saucerWall > 6f || saucerHeight < 4f ||
saucerHeight > 25f || saucerBase < 1.2f || saucerBase > 5f))))))
    throw IOException("Topf-, Boden- oder AuffangschalenmaÃŸe sind ungÃ¼ltig.")
val bottomOuter = bottomDiameter / 2f
val topOuter = topDiameter / 2f
val bottomInner = bottomOuter - wall
val topInner = topOuter - wall
val drainRadius = drainDiameter / 2f
if (bottomInner <= drainRadius + 2f || topInner <= 2f)
    throw IOException("WandstÃ¤rke und AbflussÃ¶ffnung lassen keinen stabilen Topfboden Ã¼brig.")
val saucerInner = bottomOuter + saucerClearance
val saucerOuter = saucerInner + saucerWall
val saucerX = bottomOuter + saucerOuter + 6f
if (includeSaucer && saucerX + saucerOuter > 200f)
    throw IOException("Topf und Auffangschale passen mit diesen MaÃŸen nicht nebeneinander auf eine Druckplatte.")
val segments = 64
val potFacets = 12L * segments
val saucerFacets = if (includeSaucer) 8L * segments else 0L
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer tapered planter and saucer", potFacets + saucerFacets)
writeTaperedPotMesh(out, bottomOuter, topOuter, bottomInner, topInner,
height, bottomThickness, drainRadius, segments)
if (includeSaucer)
writeSaucerMesh(out, saucerX, saucerInner, saucerOuter,
saucerHeight, saucerBase, segments) })
}
/** Writes a one-piece phone stand with a leaned back rib and an open cable channel. */
 @Throws(IOException::class)
@JvmStatic
fun writePhoneStand(file:File, width:Float, phoneThickness:Float,
tiltDegrees:Float, depth:Float, backHeight:Float,
baseThickness:Float, lipHeight:Float,
cableGap:Float, cableDepth:Float) {
if ((!java.lang.Float.isFinite(width) || !java.lang.Float.isFinite(phoneThickness) ||
!java.lang.Float.isFinite(tiltDegrees) || !java.lang.Float.isFinite(depth) ||
!java.lang.Float.isFinite(backHeight) || !java.lang.Float.isFinite(baseThickness) ||
!java.lang.Float.isFinite(lipHeight) || !java.lang.Float.isFinite(cableGap) ||
!java.lang.Float.isFinite(cableDepth) || width < 55f || width > 140f ||
phoneThickness < 4f || phoneThickness > 18f || tiltDegrees < 10f || tiltDegrees > 35f ||
depth < 65f || depth > 160f || backHeight < 50f || backHeight > 140f ||
baseThickness < 4f || baseThickness > 12f || lipHeight < 8f || lipHeight > 28f ||
cableGap < 8f || cableGap > 30f || cableGap > width - 20f ||
cableDepth < 12f || cableDepth > 40f || cableDepth > depth - 18f))
    throw IOException("Smartphone-StÃ¤nder-MaÃŸe liegen auÃŸerhalb des zulÃ¤ssigen Bereichs.")
val lipDepth = Math.max(12f, phoneThickness + 4f)
val ribDepth = Math.max(7f, phoneThickness + 2f)
val angle = (Math.toRadians((tiltDegrees).toDouble())).toFloat()
val ribBottomY = depth - (backHeight - baseThickness) * (Math.tan((angle).toDouble())).toFloat()
if (ribBottomY - ribDepth < lipDepth + 10f)
    throw IOException("StandfuÃŸtiefe und Neigung lassen zu wenig Platz fÃ¼r die GerÃ¤teauflage.")
val railWidth = (width - cableGap) / 2f
val facets = 6L * 12L
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer adjustable phone stand", facets)
// Two feet and a rear crossbar leave a cable path through the front half.
 writeBoxAt(out, 0f, 0f, 0f, railWidth, depth, baseThickness)
writeBoxAt(out, width - railWidth, 0f, 0f, width, depth, baseThickness)
writeBoxAt(out, 0f, cableDepth, 0f, width, depth, baseThickness)
// The retaining lip has the same cable notch as the base.
 writeBoxAt(out, 0f, 0f, baseThickness, railWidth, lipDepth, lipHeight)
writeBoxAt(out, width - railWidth, 0f, baseThickness, width, lipDepth, lipHeight)
writeYzPrism(out, width, ribBottomY, ribDepth, baseThickness, depth, backHeight) })
}
/** Writes a low-profile screw-mounted cradle tray for a power strip. */
 @Throws(IOException::class)
@JvmStatic
fun writePowerStripTray(file:File, bodyLength:Float, bodyWidth:Float,
bodyHeight:Float, clearance:Float, baseThickness:Float,
endStopHeight:Float, cableNotchWidth:Float,
screwHole:Float) {
if ((!java.lang.Float.isFinite(bodyLength) || !java.lang.Float.isFinite(bodyWidth) ||
!java.lang.Float.isFinite(bodyHeight) || !java.lang.Float.isFinite(clearance) ||
!java.lang.Float.isFinite(baseThickness) || !java.lang.Float.isFinite(endStopHeight) ||
!java.lang.Float.isFinite(cableNotchWidth) || !java.lang.Float.isFinite(screwHole) ||
bodyLength < 80f || bodyLength > 300f || bodyWidth < 30f || bodyWidth > 100f ||
bodyHeight < 10f || bodyHeight > 60f || clearance < 0.5f || clearance > 5f ||
baseThickness < 2f || baseThickness > 6f || endStopHeight < 8f || endStopHeight > 25f ||
cableNotchWidth < 8f || cableNotchWidth > 30f ||
cableNotchWidth > bodyWidth + 2f * clearance - 10f ||
screwHole < 3f || screwHole > 8f))
    throw IOException("MaÃŸe fÃ¼r Mehrfachsteckdosen-Halter oder SchraublÃ¶cher sind ungÃ¼ltig.")
val sideWall = 3.5f
val innerWidth = bodyWidth + 2f * clearance
val trayWidth = innerWidth + 2f * sideWall
val innerLength = bodyLength + 2f * clearance
if (innerWidth <= cableNotchWidth + 10f || innerLength < 80f)
    throw IOException("Kabelausschnitt oder HalterflÃ¤che ist fÃ¼r die Steckdose zu groÃŸ.")
val lipThickness = 3.5f
val lipTop = baseThickness + endStopHeight
val halfGap = (trayWidth - cableNotchWidth) / 2f
val earWidth = 16f
val earDepth = 20f
val leftEarX = -14f
val rightEarX = trayWidth - 2f
val sideWallHeight = Math.min(12f, bodyHeight * 0.35f)
val facets = 12L * 6L + 4L * (6L * 32L + 8L)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer power strip mounting tray", facets)
writeBoxAt(out, 0f, 0f, 0f, trayWidth, innerLength, baseThickness)
writeBoxAt(out, 0f, 0f, baseThickness, trayWidth, lipThickness, lipTop)
writeBoxAt(out, 0f, innerLength - lipThickness, baseThickness,
halfGap, innerLength, lipTop)
writeBoxAt(out, halfGap + cableNotchWidth, innerLength - lipThickness, baseThickness,
trayWidth, innerLength, lipTop)
writeBoxAt(out, 0f, 0f, baseThickness, sideWall, innerLength,
baseThickness + sideWallHeight)
writeBoxAt(out, trayWidth - sideWall, 0f, baseThickness, trayWidth, innerLength,
baseThickness + sideWallHeight)
writeFootRingAt(out, leftEarX, 0f, earWidth, earDepth, screwHole, baseThickness)
writeFootRingAt(out, rightEarX, 0f, earWidth, earDepth, screwHole, baseThickness)
writeFootRingAt(out, leftEarX, innerLength - earDepth, earWidth, earDepth, screwHole, baseThickness)
writeFootRingAt(out, rightEarX, innerLength - earDepth, earWidth, earDepth, screwHole, baseThickness) })
}
@Throws(IOException::class)
private fun writeYzPrism(out:OutputStream, width:Float, bottomY:Float,
thickness:Float, bottomZ:Float, topY:Float, topZ:Float) {
val yz = arrayOf<FloatArray>(floatArrayOf(bottomY, bottomZ), floatArrayOf(topY, topZ), floatArrayOf(topY - thickness, topZ), floatArrayOf(bottomY - thickness, bottomZ))
// End caps (the +X cap faces outward; the -X cap uses reversed winding).
 facet(out, 0f, yz[0][0], yz[0][1], 0f, yz[2][0], yz[2][1], 0f, yz[1][0], yz[1][1])
facet(out, 0f, yz[0][0], yz[0][1], 0f, yz[3][0], yz[3][1], 0f, yz[2][0], yz[2][1])
facet(out, width, yz[0][0], yz[0][1], width, yz[1][0], yz[1][1], width, yz[2][0], yz[2][1])
facet(out, width, yz[0][0], yz[0][1], width, yz[2][0], yz[2][1], width, yz[3][0], yz[3][1])
for (i in 0..3)
{
val n = (i + 1) % 4
quad(out, 0f, yz[i][0], yz[i][1], 0f, yz[n][0], yz[n][1],
width, yz[n][0], yz[n][1], width, yz[i][0], yz[i][1], true)
}
}
@Throws(IOException::class)
private fun writeTaperedPotMesh(out:OutputStream, outerBottom:Float, outerTop:Float,
innerBottom:Float, innerTop:Float, height:Float,
base:Float, drainRadius:Float, segments:Int) {
for (i in 0 until segments)
{
val n = (i + 1) % segments
val a0 = Math.PI * 2.0 * i / segments
val a1 = Math.PI * 2.0 * n / segments
val c0 = (Math.cos(a0)).toFloat()
val s0 = (Math.sin(a0)).toFloat()
val c1 = (Math.cos(a1)).toFloat()
val s1 = (Math.sin(a1)).toFloat()
quad(out, c0 * outerBottom, s0 * outerBottom, 0f, c1 * outerBottom, s1 * outerBottom, 0f,
c1 * outerTop, s1 * outerTop, height, c0 * outerTop, s0 * outerTop, height, true)
quad(out, c0 * innerBottom, s0 * innerBottom, base, c1 * innerBottom, s1 * innerBottom, base,
c1 * innerTop, s1 * innerTop, height, c0 * innerTop, s0 * innerTop, height, false)
quad(out, c0 * outerTop, s0 * outerTop, height, c1 * outerTop, s1 * outerTop, height,
c1 * innerTop, s1 * innerTop, height, c0 * innerTop, s0 * innerTop, height, true)
quad(out, c0 * outerBottom, s0 * outerBottom, 0f, c1 * outerBottom, s1 * outerBottom, 0f,
c1 * drainRadius, s1 * drainRadius, 0f, c0 * drainRadius, s0 * drainRadius, 0f, false)
quad(out, c0 * outerBottom, s0 * outerBottom, base, c1 * outerBottom, s1 * outerBottom, base,
c1 * drainRadius, s1 * drainRadius, base, c0 * drainRadius, s0 * drainRadius, base, true)
quad(out, c0 * drainRadius, s0 * drainRadius, 0f, c1 * drainRadius, s1 * drainRadius, 0f,
c1 * drainRadius, s1 * drainRadius, base, c0 * drainRadius, s0 * drainRadius, base, false)
}
}
@Throws(IOException::class)
private fun writeSaucerMesh(out:OutputStream, centerX:Float, innerRadius:Float,
outerRadius:Float, height:Float, base:Float, segments:Int) {
for (i in 0 until segments)
{
val n = (i + 1) % segments
val a0 = Math.PI * 2.0 * i / segments
val a1 = Math.PI * 2.0 * n / segments
val c0 = (Math.cos(a0)).toFloat()
val s0 = (Math.sin(a0)).toFloat()
val c1 = (Math.cos(a1)).toFloat()
val s1 = (Math.sin(a1)).toFloat()
val x0 = centerX + c0 * outerRadius
val y0 = s0 * outerRadius
val x1 = centerX + c1 * outerRadius
val y1 = s1 * outerRadius
val ix0 = centerX + c0 * innerRadius
val iy0 = s0 * innerRadius
val ix1 = centerX + c1 * innerRadius
val iy1 = s1 * innerRadius
quad(out, x0, y0, 0f, x1, y1, 0f, x1, y1, height, x0, y0, height, true)
quad(out, ix0, iy0, base, ix1, iy1, base, ix1, iy1, height, ix0, iy0, height, false)
quad(out, x0, y0, height, x1, y1, height, ix1, iy1, height, ix0, iy0, height, true)
facet(out, centerX, 0f, 0f, x1, y1, 0f, x0, y0, 0f)
facet(out, centerX, 0f, base, x0, y0, base, x1, y1, base)
}
}
@Throws(IOException::class)
private fun writeBoxWalls(out:OutputStream, w:Float, d:Float, t:Float,
floor:Float, h:Float) {
quad(out, 0f, 0f, floor, w, 0f, floor, w, 0f, h, 0f, 0f, h, true) // outside front
quad(out, w, d, floor, 0f, d, floor, 0f, d, h, w, d, h, true) // outside back
quad(out, 0f, d, floor, 0f, 0f, floor, 0f, 0f, h, 0f, d, h, true) // outside left
quad(out, w, 0f, floor, w, d, floor, w, d, h, w, 0f, h, true) // outside right
quad(out, t, t, floor, t, t, h, w - t, t, h, w - t, t, floor, true) // inside front
quad(out, w - t, d - t, floor, w - t, d - t, h, t, d - t, h, t, d - t, floor, true)// inside back
quad(out, t, d - t, floor, t, d - t, h, t, t, h, t, t, floor, true) // inside left
quad(out, w - t, t, floor, w - t, t, h, w - t, d - t, h, w - t, d - t, floor, true)// inside right
}
@Throws(IOException::class)
private fun quad(out:OutputStream, ax:Float, ay:Float, az:Float, bx:Float, by:Float, bz:Float,
cx:Float, cy:Float, cz:Float, dx:Float, dy:Float, dz:Float, outward:Boolean) {
if (outward)
{
facet(out, ax, ay, az, bx, by, bz, cx, cy, cz)
facet(out, ax, ay, az, cx, cy, cz, dx, dy, dz)
}
else
{
facet(out, ax, ay, az, cx, cy, cz, bx, by, bz)
facet(out, ax, ay, az, dx, dy, dz, cx, cy, cz)
}
}
@Throws(IOException::class)
private fun writeBoxRim(out:OutputStream, x:Float, y:Float, width:Float, depth:Float,
wall:Float, z:Float, top:Boolean) {
val x1 = x + width
val y1 = y + depth
writeRectFace(out, x, y, x1, y + wall, z, top)
writeRectFace(out, x1 - wall, y + wall, x1, y1 - wall, z, top)
writeRectFace(out, x, y1 - wall, x1, y1, z, top)
writeRectFace(out, x, y + wall, x + wall, y1 - wall, z, top)
}
@Throws(IOException::class)
private fun writeRectFace(out:OutputStream, x0:Float, y0:Float, x1:Float, y1:Float, z:Float, top:Boolean) {
if (top)
{
facet(out, x0, y0, z, x1, y0, z, x1, y1, z)
facet(out, x0, y0, z, x1, y1, z, x0, y1, z)
}
else
{
facet(out, x0, y0, z, x1, y1, z, x1, y0, z)
facet(out, x0, y0, z, x0, y1, z, x1, y1, z)
}
}
@Throws(IOException::class)
private fun writeLid(out:OutputStream, x:Float, y:Float, outerW:Float, outerD:Float,
wall:Float, skirt:Float, roof:Float) {
val x1 = x + outerW
val y1 = y + outerD
val zTop = skirt + roof
val ix0 = x + wall
val iy0 = y + wall
val ix1 = x1 - wall
val iy1 = y1 - wall
// Outside walls, then the inward-facing surfaces of the lid's skirt.
 quad(out, x, y, 0f, x1, y, 0f, x1, y, zTop, x, y, zTop, true)
quad(out, x1, y1, 0f, x, y1, 0f, x, y1, zTop, x1, y1, zTop, true)
quad(out, x, y1, 0f, x, y, 0f, x, y, zTop, x, y1, zTop, true)
quad(out, x1, y, 0f, x1, y1, 0f, x1, y1, zTop, x1, y, zTop, true)
quad(out, ix0, iy0, 0f, ix0, iy0, skirt, ix1, iy0, skirt, ix1, iy0, 0f, true)
quad(out, ix1, iy1, 0f, ix1, iy1, skirt, ix0, iy1, skirt, ix0, iy1, 0f, true)
quad(out, ix0, iy1, 0f, ix0, iy1, skirt, ix0, iy0, skirt, ix0, iy0, 0f, true)
quad(out, ix1, iy0, 0f, ix1, iy0, skirt, ix1, iy1, skirt, ix1, iy1, 0f, true)
writeRectFace(out, x, y, x1, y1, zTop, true)
writeRectFace(out, ix0, iy0, ix1, iy1, skirt, false)
writeBoxRim(out, x, y, outerW, outerD, wall, 0f, false)
}
/** Writes a lithophane panel with a raised perimeter and a flat stand foot with a cable opening. */
 @Throws(IOException::class)
@JvmStatic
fun writeFramedPanel(file:File, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float, frameDepth:Float,
footDepth:Float, openingDiameter:Float, rimWidth:Float) {
validateGrid(topZ, cols, rows, width, height)
if ((!java.lang.Float.isFinite(frameDepth) || frameDepth <= 0f ||
!java.lang.Float.isFinite(footDepth) || footDepth <= 0f ||
!java.lang.Float.isFinite(openingDiameter) || openingDiameter < 0f ||
!java.lang.Float.isFinite(rimWidth) || rimWidth <= 0f || rimWidth * 2f >= width ||
openingDiameter >= Math.min(width * 0.8f, footDepth - 2f)))
    throw IOException("Rahmen, StandfuÃŸ oder KabelÃ¶ffnung haben ungÃ¼ltige MaÃŸe.")
val facets = panelFacetCount(cols, rows) + 4L * 12L + (if (openingDiameter > 0f) 6L * 32L + 8L else 12L)
if (facets > 0xffffffffL) throw IOException("Das STL-Modell ist zu groÃŸ.")
var maxZ = frameDepth
for (value in topZ) maxZ = Math.max(maxZ, value)
val railTop = maxZ + 1.2f
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer framed Lithophane with stand", facets)
writePanelMesh(out, topZ, cols, rows, width, height, 0f, footDepth)
// Four slightly overlapping bezel rails hold the image edge and protect it.
 writeBox(out, -rimWidth, footDepth - rimWidth, 0f, width + rimWidth,
footDepth + rimWidth, railTop)
writeBox(out, -rimWidth, footDepth + height - rimWidth, 0f, width + rimWidth,
footDepth + height + rimWidth, railTop)
writeBox(out, -rimWidth, footDepth, 0f, rimWidth, footDepth + height, railTop)
writeBox(out, width - rimWidth, footDepth, 0f, width + rimWidth, footDepth + height, railTop)
if (openingDiameter > 0f)
writeFootRing(out, width, footDepth, openingDiameter, frameDepth)
else
writeBox(out, 0f, 0f, 0f, width, footDepth, frameDepth) })
}
@Throws(IOException::class)
private fun writeBox(out:OutputStream, x0:Float, y0:Float, z0:Float,
x1:Float, y1:Float, z1:Float) {
writeBoxAt(out, x0, y0, z0, x1, y1, z1)
}
@Throws(IOException::class)
private fun writeBoxAt(out:OutputStream, x0:Float, y0:Float, z0:Float,
x1:Float, y1:Float, z1:Float) {
val v = arrayOf<FloatArray>(floatArrayOf(x0, y0, z0), floatArrayOf(x1, y0, z0), floatArrayOf(x1, y1, z0), floatArrayOf(x0, y1, z0), floatArrayOf(x0, y0, z1), floatArrayOf(x1, y0, z1), floatArrayOf(x1, y1, z1), floatArrayOf(x0, y1, z1))
val f = arrayOf<IntArray>(intArrayOf(0, 2, 1), intArrayOf(0, 3, 2), intArrayOf(4, 5, 6), intArrayOf(4, 6, 7), intArrayOf(0, 1, 5), intArrayOf(0, 5, 4), intArrayOf(1, 2, 6), intArrayOf(1, 6, 5), intArrayOf(2, 3, 7), intArrayOf(2, 7, 6), intArrayOf(3, 0, 4), intArrayOf(3, 4, 7))
for (face in f)
    facet(out, v[face[0]][0], v[face[0]][1], v[face[0]][2],
v[face[1]][0], v[face[1]][1], v[face[1]][2], v[face[2]][0], v[face[2]][1], v[face[2]][2])
}
/** Rectangular annulus with a circular vertical cable opening; all surfaces are closed. */
 @Throws(IOException::class)
private fun writeFootRing(out:OutputStream, width:Float, depth:Float, diameter:Float, thickness:Float) {
writeFootRingAt(out, 0f, 0f, width, depth, diameter, thickness)
}
@Throws(IOException::class)
private fun writeFootRingAt(out:OutputStream, offsetX:Float, offsetY:Float,
width:Float, depth:Float, diameter:Float, thickness:Float) {
writeFootRingAt(out, offsetX, offsetY, width, depth, diameter, 0f, thickness)
}
@Throws(IOException::class)
private fun writeFootRingAt(out:OutputStream, offsetX:Float, offsetY:Float,
width:Float, depth:Float, diameter:Float, z0:Float, z1:Float) {
val segments = 32
val cx = width / 2f
val cy = depth / 2f
val radius = diameter / 2f
val ix = FloatArray(segments)
val iy = FloatArray(segments)
val ox = FloatArray(segments)
val oy = FloatArray(segments)
for (i in 0 until segments)
{
val angle = Math.PI * 2.0 * i / segments
val dx = (Math.cos(angle)).toFloat()
val dy = (Math.sin(angle)).toFloat()
ix[i] = offsetX + cx + dx * radius
iy[i] = offsetY + cy + dy * radius
val scale = Math.min((width / 2f) / Math.abs(if (dx == 0f) 1e-6f else dx),
(depth / 2f) / Math.abs(if (dy == 0f) 1e-6f else dy))
ox[i] = offsetX + cx + dx * scale
oy[i] = offsetY + cy + dy * scale
}
for (i in 0 until segments)
{
val n = (i + 1) % segments
facet(out, ox[i], oy[i], z0, ix[n], iy[n], z0, ox[n], oy[n], z0)
facet(out, ox[i], oy[i], z0, ix[i], iy[i], z0, ix[n], iy[n], z0)
facet(out, ox[i], oy[i], z1, ox[n], oy[n], z1, ix[n], iy[n], z1)
facet(out, ox[i], oy[i], z1, ix[n], iy[n], z1, ix[i], iy[i], z1)
facet(out, ix[i], iy[i], z0, ix[n], iy[n], z1, ix[n], iy[n], z0)
facet(out, ix[i], iy[i], z0, ix[i], iy[i], z1, ix[n], iy[n], z1)
}
val outerX0 = offsetX
val outerY0 = offsetY
val outerX1 = offsetX + width
val outerY1 = offsetY + depth
val sides = arrayOf<FloatArray>(floatArrayOf(outerX0, outerY0, outerX1, outerY0), floatArrayOf(outerX1, outerY0, outerX1, outerY1), floatArrayOf(outerX1, outerY1, outerX0, outerY1), floatArrayOf(outerX0, outerY1, outerX0, outerY0))
for (side in sides)
{
val x0 = side[0]
val y0 = side[1]
val x1 = side[2]
val y1 = side[3]
facet(out, x0, y0, z0, x1, y1, z0, x1, y1, z1)
facet(out, x0, y0, z0, x1, y1, z1, x0, y0, z1)
}
}
/**
 * Creates four separate, flat-printable image panels and one square base. The pieces are
 * arranged in a 2x2 group with the base beside them; after printing, glue the four panels
 * upright around the base to form an open-top illuminated photo cube.
 */
 @Throws(IOException::class)
@JvmStatic
fun writeCubeKit(file:File, panelTopZ:List<FloatArray>, cols:Int, rows:Int,
width:Float, height:Float, baseThickness:Float,
gap:Float) {
if (panelTopZ == null || panelTopZ.size != 4)
    throw IOException("FÃ¼r den Lithophan-WÃ¼rfel werden genau vier Bilder benÃ¶tigt.")
for (grid in panelTopZ) validateGrid(grid, cols, rows, width, height)
if ((!java.lang.Float.isFinite(baseThickness) || baseThickness <= 0f ||
!java.lang.Float.isFinite(gap) || gap < 0f))
    throw IOException("UngÃ¼ltige WÃ¼rfelmaÃŸe.")
val panelFacets = panelFacetCount(cols, rows)
val facets = panelFacets * 4L + panelFacetCount(2, 2)
if (facets > 0xffffffffL) throw IOException("Das STL-Modell ist zu groÃŸ.")
// Pack by the real rotated wall bounds. Using width for both steps caused tall panels
 // to overlap in the former layout, which could confuse mesh repair and slicing.
 val stepX = Math.max(width, height) + gap
val stepY = Math.max(width, height) + gap
val baseSide = Math.min(width, height)
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer four-photo Lithophane cube kit", facets)
writePanelMesh(out, panelTopZ.get(0), cols, rows, width, height, 0f, 0f)
writePanelMesh(out, transpose(panelTopZ.get(1), cols, rows), rows, cols, height, width, stepX, 0f)
writePanelMesh(out, panelTopZ.get(2), cols, rows, width, height, stepX, stepY)
writePanelMesh(out, transpose(panelTopZ.get(3), cols, rows), rows, cols, height, width, 0f, stepY)
val base = floatArrayOf(baseThickness, baseThickness, baseThickness, baseThickness)
writePanelMesh(out, base, 2, 2, baseSide, baseSide, 0f, stepY * 2f) })
}
/** LitoGEN: four photo walls, a base and a separate flat lid, optionally using a fifth image. */
 @Throws(IOException::class)
@JvmStatic
fun writeLitoGenCubeKit(file:File, sideTopZ:List<FloatArray>, lidTopZ:FloatArray?,
cols:Int, rows:Int, lidCols:Int, lidRows:Int,
width:Float, height:Float, baseThickness:Float,
gap:Float) {
if (sideTopZ == null || sideTopZ.size != 4)
    throw IOException("LitoGEN benÃ¶tigt genau vier Seitenbilder.")
for (panel in sideTopZ) validateGrid(panel, cols, rows, width, height)
if ((!java.lang.Float.isFinite(baseThickness) || baseThickness < 0.8f || baseThickness > 5f ||
!java.lang.Float.isFinite(gap) || gap < 2f || gap > 20f))
    throw IOException("BodenstÃ¤rke oder Abstand ist auÃŸerhalb des sicheren Bereichs.")
val square = width
val lid = if (lidTopZ == null)
floatArrayOf(baseThickness, baseThickness, baseThickness, baseThickness)
else
lidTopZ
val lc = if (lidTopZ == null) 2 else lidCols
val lr = if (lidTopZ == null) 2 else lidRows
validateGrid(lid, lc, lr, square, square)
val panelFacets = panelFacetCount(cols, rows)
val lidFacets = panelFacetCount(lc, lr)
val facets = panelFacets * 4L + panelFacetCount(2, 2) + lidFacets
if (facets > 0xffffffffL) throw IOException("Das LitoGEN-Modell ist zu groÃŸ.")
val step = Math.max(width, height) + gap
BufferedOutputStream(FileOutputStream(file)).use({ out->
writeHeader(out, "OrkanSlicer LitoGEN photo cube kit", facets)
writePanelMesh(out, sideTopZ.get(0), cols, rows, width, height, 0f, 0f)
writePanelMesh(out, transpose(sideTopZ.get(1), cols, rows), rows, cols, height, width, step, 0f)
writePanelMesh(out, sideTopZ.get(2), cols, rows, width, height, step, step)
writePanelMesh(out, transpose(sideTopZ.get(3), cols, rows), rows, cols, height, width, 0f, step)
val base = floatArrayOf(baseThickness, baseThickness, baseThickness, baseThickness)
writePanelMesh(out, base, 2, 2, square, square, 0f, step * 2f)
writePanelMesh(out, lid, lc, lr, square, square, step, step * 2f) })
}
private fun transpose(source:FloatArray, cols:Int, rows:Int):FloatArray {
val transposed = FloatArray(source.size)
for (y in 0 until rows)
    for (x in 0 until cols) transposed[x * rows + y] = source[y * cols + x]
return transposed
}
@Throws(IOException::class)
private fun validateGrid(topZ:FloatArray, cols:Int, rows:Int, width:Float, height:Float) {
if (topZ == null || cols < 2 || rows < 2 || cols.toLong() * rows != topZ.size.toLong())
    throw IOException("Das Lithophan-Bildraster ist ungÃ¼ltig.")
if (!java.lang.Float.isFinite(width) || !java.lang.Float.isFinite(height) || width <= 0f || height <= 0f)
    throw IOException("Breite und HÃ¶he mÃ¼ssen grÃ¶ÃŸer als 0 sein.")
for (value in topZ)
{
if (!java.lang.Float.isFinite(value) || value <= 0f)
    throw IOException("Die berechnete WandstÃ¤rke ist ungÃ¼ltig.")
}
}
private fun panelFacetCount(cols:Int, rows:Int):Long {
return ((cols - 1).toLong() * (rows - 1).toLong() * 4L + 2L * ((cols - 1L) * 2L + (rows - 1L) * 2L))
}
@Throws(IOException::class)
private fun writeHeader(out:OutputStream, name:String, facets:Long) {
val header = ByteArray(80)
val label = name.toByteArray(StandardCharsets.US_ASCII)
System.arraycopy(label, 0, header, 0, Math.min(label.size, header.size))
out.write(header)
writeIntLE(out, facets.toInt())
}
@Throws(IOException::class)
private fun writePanelMesh(out:OutputStream, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float, offsetX:Float, offsetY:Float) {
val dx = width / (cols - 1)
val dy = height / (rows - 1)
for (y in 0 until rows - 1)
{
for (x in 0 until cols - 1)
{
val x0 = offsetX + x * dx
val x1 = offsetX + (x + 1) * dx
val y0 = offsetY + y * dy
val y1 = offsetY + (y + 1) * dy
val z00 = topZ[y * cols + x]
val z10 = topZ[y * cols + x + 1]
val z01 = topZ[(y + 1) * cols + x]
val z11 = topZ[(y + 1) * cols + x + 1]
facet(out, x0, y0, z00, x1, y0, z10, x1, y1, z11)
facet(out, x0, y0, z00, x1, y1, z11, x0, y1, z01)
facet(out, x0, y0, 0f, x1, y1, 0f, x1, y0, 0f)
facet(out, x0, y0, 0f, x0, y1, 0f, x1, y1, 0f)
}
}
writeOuterWalls(out, topZ, cols, rows, width, height, offsetX, offsetY)
}
@Throws(IOException::class)
private fun writeOuterWalls(out:OutputStream, topZ:FloatArray, cols:Int, rows:Int,
width:Float, height:Float, offsetX:Float, offsetY:Float) {
val dx = width / (cols - 1)
val dy = height / (rows - 1)
for (x in 0 until cols - 1)
    wall(out, offsetX + x * dx, offsetY, offsetX + (x + 1) * dx, offsetY,
topZ[x], topZ[x + 1])
for (y in 0 until rows - 1)
    wall(out, offsetX + width, offsetY + y * dy, offsetX + width, offsetY + (y + 1) * dy,
topZ[y * cols + cols - 1], topZ[(y + 1) * cols + cols - 1])
for (x in cols - 1 downTo 1)
    wall(out, offsetX + x * dx, offsetY + height, offsetX + (x - 1) * dx,
offsetY + height, topZ[(rows - 1) * cols + x], topZ[(rows - 1) * cols + x - 1])
for (y in rows - 1 downTo 1)
    wall(out, offsetX, offsetY + y * dy, offsetX, offsetY + (y - 1) * dy,
topZ[y * cols], topZ[(y - 1) * cols])
}
@Throws(IOException::class)
private fun wall(out:OutputStream, x0:Float, y0:Float, x1:Float, y1:Float,
z0:Float, z1:Float) {
facet(out, x0, y0, 0f, x1, y1, 0f, x1, y1, z1)
facet(out, x0, y0, 0f, x1, y1, z1, x0, y0, z0)
}
@Throws(IOException::class)
private fun facet(out:OutputStream, ax:Float, ay:Float, az:Float,
bx:Float, by:Float, bz:Float, cx:Float, cy:Float, cz:Float) {
val ux = bx - ax
val uy = by - ay
val uz = bz - az
val vx = cx - ax
val vy = cy - ay
val vz = cz - az
var nx = uy * vz - uz * vy
var ny = uz * vx - ux * vz
var nz = ux * vy - uy * vx
val length = (Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble())).toFloat()
if (length > 0f) {
nx /= length
ny /= length
nz /= length
}
writeFloatLE(out, nx)
writeFloatLE(out, ny)
writeFloatLE(out, nz)
writeFloatLE(out, ax)
writeFloatLE(out, ay)
writeFloatLE(out, az)
writeFloatLE(out, bx)
writeFloatLE(out, by)
writeFloatLE(out, bz)
writeFloatLE(out, cx)
writeFloatLE(out, cy)
writeFloatLE(out, cz)
out.write(0)
out.write(0)
}
@Throws(IOException::class)
private fun writeFloatLE(out:OutputStream, value:Float) {
val bits = java.lang.Float.floatToIntBits(value)
out.write(bits)
out.write(bits.ushr(8))
out.write(bits.ushr(16))
out.write(bits.ushr(24))
}
@Throws(IOException::class)
private fun writeIntLE(out:OutputStream, value:Int) {
out.write(value)
out.write(value.ushr(8))
out.write(value.ushr(16))
out.write(value.ushr(24))
}
}/** Writes a screw-mounted tool rack with raised guide collars around every tool opening. */
