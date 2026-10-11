package ru.ytkab0bp.slicebeam.utils

import ru.ytkab0bp.slicebeam.config.ConfigObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Builds the single-plate QIDI print package expected by the Q2 Moonraker workflow. */
object QidiGCode3mfPackager {
    private const val PLATE = 1
    private val settingLine = Regex("^;\\s*([^=]+?)\\s*=\\s*(.*?)\\s*$")
    private val layerLine = Regex("(?i)^;\\s*total layer number\\s*:\\s*(\\d+)\\s*$")
    private val objectLine = Regex("(?i)^EXCLUDE_OBJECT_DEFINE\\s+NAME=([^\\s]+).*?POLYGON=(.*)$")
    private val number = Regex("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)")
    private val durationPart = Regex("(\\d+(?:\\.\\d+)?)\\s*([dhms])", RegexOption.IGNORE_CASE)
    private val thumbnailBegin = Regex("(?i)^;\\s*thumbnail\\s+begin\\s+(\\d+)x(\\d+)\\s+(\\d+)(?:\\s+.*)?$")

    data class PackageSummary(
        val layers: Int,
        val durationSeconds: Int,
        val filamentCount: Int,
        val thumbnailBytes: Int,
    )

    private data class ObjectBounds(val name: String, val minX: Double, val minY: Double, val maxX: Double, val maxY: Double) {
        val area: Double get() = ((maxX - minX) * (maxY - minY)).coerceAtLeast(0.0)
    }

    private data class PrintMetadata(
        val settings: Map<String, String>,
        val layers: Int,
        val seconds: Int,
        val firstLayerSeconds: Double,
        val nozzle: Double,
        val materials: List<String>,
        val colors: List<String>,
        val usedMeters: List<Double>,
        val usedGrams: List<Double>,
        val filamentIds: List<String>,
        val filamentMaps: List<String>,
        val sequence: List<Int>,
        val objects: List<ObjectBounds>,
    )

    @JvmStatic
    fun isQidiQ2(printer: ConfigObject?): Boolean {
        if (printer == null) return false
        val model = printer.get("printer_model").orEmpty().trim()
        val profile = printer.title.orEmpty().trim()
        return isQidiQ2Name(model) || isQidiQ2Name(profile)
    }

    private fun isQidiQ2Name(value: String): Boolean =
        Regex("(?i)^(?:qidi\\s*)?q2(?:\\s|$|[._-]).*").matches(value) ||
            Regex("(?i)^(?:qidi\\s*)?q2$").matches(value)

    @JvmStatic
    fun uploadName(recommendedName: String): String {
        val base = recommendedName.trim().ifEmpty { "print.gcode" }
        return if (base.endsWith(".gcode.3mf", ignoreCase = true)) base
        else if (base.endsWith(".gcode", ignoreCase = true)) base + ".3mf"
        else "$base.gcode.3mf"
    }

    /** Creates an OPC/3MF zip with QIDI's plate paths, summary metadata, and a real PNG preview. */
    @JvmStatic
    @Throws(Exception::class)
    fun create(
        sourceGcode: File,
        destination: File,
        previewPng: ByteArray?,
        printer: ConfigObject,
        filamentSlots: List<FilamentSlot>,
        clientVersion: String,
        displayName: String,
    ): PackageSummary {
        require(isQidiQ2(printer)) { "This package format is only enabled for QIDI Q2 printers." }
        require(sourceGcode.isFile && sourceGcode.length() > 0L) { "The sliced G-code file is missing." }
        val metadata = readMetadata(sourceGcode, filamentSlots)
        require(metadata.layers > 0) { "The sliced file does not declare a valid layer count." }
        val thumbnail = previewPng?.takeIf(::isPng) ?: readGcodeThumbnail(sourceGcode)
            ?: throw IllegalArgumentException("The QIDI Q2 print package needs a model preview image.")

        destination.parentFile?.mkdirs()
        if (destination.exists() && !destination.delete()) throw java.io.IOException("Could not replace the previous QIDI package.")
        val packageGcodePath = "Metadata/plate_$PLATE.gcode"
        val settingsJson = settingsJson(metadata, printer)
        val summaryXml = sliceInfoXml(metadata, printer, clientVersion)
        val modelSettingsXml = modelSettingsXml(metadata)
        val md5 = digest(sourceGcode)
        try {
            ZipOutputStream(FileOutputStream(destination)).use { zip ->
                writeText(zip, "[Content_Types].xml", contentTypesXml)
                writeText(zip, "_rels/.rels", rootRelationshipsXml)
                writeText(zip, "3D/3dmodel.model", modelXml(clientVersion, displayName))
                writeBytes(zip, "Metadata/plate_$PLATE.png", thumbnail)
                writeBytes(zip, "Metadata/plate_no_light_$PLATE.png", thumbnail)
                writeBytes(zip, "Metadata/top_$PLATE.png", thumbnail)
                writeBytes(zip, "Metadata/pick_$PLATE.png", thumbnail)
                writeText(zip, "Metadata/plate_$PLATE.json", plateJson(metadata))
                writeText(zip, "Metadata/project_settings.config", settingsJson)
                writeText(zip, "Metadata/plate_$PLATE.gcode.md5", md5)
                zip.putNextEntry(ZipEntry(packageGcodePath))
                FileInputStream(sourceGcode).use { input -> input.copyTo(zip, 128 * 1024) }
                zip.closeEntry()
                writeText(zip, "Metadata/_rels/model_settings.config.rels", modelSettingsRelationshipsXml(packageGcodePath))
                writeText(zip, "Metadata/model_settings.config", modelSettingsXml)
                writeText(zip, "Metadata/cut_information.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<objects/>\n")
                writeText(zip, "Metadata/slice_info.config", summaryXml)
                writeText(zip, "Metadata/filament_sequence.json", sequenceJson(metadata))
            }
            verify(destination, md5, thumbnail)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
        return PackageSummary(metadata.layers, metadata.seconds, metadata.materials.size, thumbnail.size)
    }

    @JvmStatic
    fun verify(packageFile: File): Boolean {
        return try {
            ZipFile(packageFile).use { zip ->
                val requiredEntries = listOf(
                    "[Content_Types].xml",
                    "_rels/.rels",
                    "3D/3dmodel.model",
                    "Metadata/_rels/model_settings.config.rels",
                    "Metadata/cut_information.xml",
                    "Metadata/filament_sequence.json",
                    "Metadata/model_settings.config",
                    "Metadata/pick_$PLATE.png",
                    "Metadata/plate_$PLATE.gcode",
                    "Metadata/plate_$PLATE.gcode.md5",
                    "Metadata/plate_$PLATE.json",
                    "Metadata/plate_$PLATE.png",
                    "Metadata/plate_no_light_$PLATE.png",
                    "Metadata/project_settings.config",
                    "Metadata/slice_info.config",
                    "Metadata/top_$PLATE.png",
                )
                if (requiredEntries.any { zip.getEntry(it) == null }) return false
                val md5Entry = zip.getEntry("Metadata/plate_$PLATE.gcode.md5") ?: return false
                val codeEntry = zip.getEntry("Metadata/plate_$PLATE.gcode") ?: return false
                val pngEntry = zip.getEntry("Metadata/plate_$PLATE.png") ?: return false
                val infoEntry = zip.getEntry("Metadata/slice_info.config") ?: return false
                val modelEntry = zip.getEntry("3D/3dmodel.model") ?: return false
                val expected = zip.getInputStream(md5Entry).bufferedReader(StandardCharsets.US_ASCII).use { it.readText() }.trim()
                val actual = zip.getInputStream(codeEntry).use { input -> digest(input) }
                val thumbnail = zip.getInputStream(pngEntry).use { input ->
                    val header = ByteArray(24)
                    var offset = 0
                    while (offset < header.size) {
                        val count = input.read(header, offset, header.size - offset)
                        if (count < 0) break
                        offset += count
                    }
                    if (offset == header.size) header else ByteArray(0)
                }
                expected.equals(actual, ignoreCase = true) && isPng(thumbnail) &&
                    infoEntry.size > 0 && modelEntry.size > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun verify(packageFile: File, expectedMd5: String, preview: ByteArray) {
        check(verify(packageFile)) { "The generated QIDI package failed its checksum or preview validation." }
        ZipFile(packageFile).use { zip ->
            val actualMd5 = zip.getInputStream(zip.getEntry("Metadata/plate_$PLATE.gcode.md5"))
                .bufferedReader(StandardCharsets.US_ASCII).use { it.readText().trim() }
            check(actualMd5.equals(expectedMd5, ignoreCase = true)) { "The QIDI G-code checksum does not match." }
            val actualPng = zip.getInputStream(zip.getEntry("Metadata/plate_$PLATE.png")).use { it.readBytes() }
            check(actualPng.contentEquals(preview)) { "The packaged QIDI preview does not match the captured image." }
        }
    }

    private fun readMetadata(file: File, slots: List<FilamentSlot>): PrintMetadata {
        val settings = LinkedHashMap<String, String>()
        val objects = LinkedHashMap<String, ObjectBounds>()
        val sequence = ArrayList<Int>()
        var layers = 0
        var seconds = 0
        var firstLayer = 0.0
        var maxTool = 0
        var thumbnailLines: MutableList<String>? = null
        var thumbnailSeen = false
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8), 64 * 1024).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (thumbnailLines != null) {
                    if (line.trim().startsWith("; thumbnail end", ignoreCase = true)) {
                        thumbnailLines = null
                    } else {
                        thumbnailLines.add(line.trim().removePrefix(";").trim())
                    }
                    continue
                }
                val thumb = thumbnailBegin.matchEntire(line.trim())
                if (thumb != null) {
                    thumbnailSeen = true
                    thumbnailLines = ArrayList()
                    continue
                }
                val layer = layerLine.matchEntire(line.trim())
                if (layer != null) layers = layer.groupValues[1].toIntOrNull() ?: layers
                val normalized = line.trim()
                val pair = settingLine.matchEntire(normalized)
                if (pair != null && settings.size < 256) {
                    settings[pair.groupValues[1].trim()] = pair.groupValues[2].trim().trim('"')
                }
                when {
                    normalized.startsWith("; estimated printing time", ignoreCase = true) -> seconds = parseDuration(normalized.substringAfter('='))
                    normalized.startsWith("; first layer time", ignoreCase = true) -> firstLayer = number.find(normalized.substringAfter('='))?.value?.toDoubleOrNull() ?: firstLayer
                    normalized.startsWith("; first_layer_time =", ignoreCase = true) -> firstLayer = number.find(normalized.substringAfter('='))?.value?.toDoubleOrNull() ?: firstLayer
                    normalized.startsWith("; filament used [mm] =", ignoreCase = true) -> settings["filament used [mm]"] = normalized.substringAfter('=').trim()
                    normalized.startsWith("; filament used [g] =", ignoreCase = true) -> settings["filament used [g]"] = normalized.substringAfter('=').trim()
                    normalized.startsWith("; total filament length [mm] :", ignoreCase = true) -> settings["total filament length [mm]"] = normalized.substringAfter(':').trim()
                    normalized.startsWith("; total filament weight [g] :", ignoreCase = true) -> settings["total filament weight [g]"] = normalized.substringAfter(':').trim()
                    normalized.startsWith("; total filament used [g] =", ignoreCase = true) -> settings["total filament used [g]"] = normalized.substringAfter('=').trim()
                }
                val tool = Regex("(?i)^T(\\d+)(?:\\s|$)").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
                if (tool != null) {
                    maxTool = maxOf(maxTool, tool)
                    val oneBased = tool + 1
                    if (sequence.lastOrNull() != oneBased) sequence.add(oneBased)
                }
                val objectMatch = objectLine.matchEntire(normalized)
                if (objectMatch != null && objectMatch.groupValues[1] !in objects) {
                    val name = objectMatch.groupValues[1].trim('"')
                    val coords = number.findAll(objectMatch.groupValues[2]).mapNotNull { it.value.toDoubleOrNull() }.toList()
                    if (coords.size >= 4) {
                        val xs = coords.filterIndexed { index, _ -> index % 2 == 0 }
                        val ys = coords.filterIndexed { index, _ -> index % 2 == 1 }
                        if (xs.isNotEmpty() && ys.isNotEmpty()) objects[name] = ObjectBounds(name, xs.min(), ys.min(), xs.max(), ys.max())
                    }
                }
            }
        }
        if (thumbnailSeen && settings["thumbnail_count"].isNullOrEmpty()) settings["thumbnail_count"] = "1"
        if (seconds == 0) seconds = parseM73Remaining(file)
        if (layers == 0) layers = settings["total layer number"]?.toIntOrNull() ?: 0

        val toolCount = maxOf(maxTool + 1, csv(settings["filament_type"]).size, csv(settings["filament_colour"]).size, 1)
        val materials = (0 until toolCount).map { i ->
            csv(settings["filament_type"]).getOrNull(i)?.ifBlank { null }
                ?: slots.getOrNull(i)?.type?.takeIf { it.isNotBlank() } ?: "PLA"
        }
        val colors = (0 until toolCount).map { i ->
            val fromGcode = csv(settings["filament_colour"]).getOrNull(i)
            fromGcode?.takeIf { it.matches(Regex("#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})")) }
                ?: slots.getOrNull(i)?.let { String.format(Locale.ROOT, "#%06X", it.color and 0xFFFFFF) }
                ?: "#FFFFFF"
        }
        val usedMeters = csv(settings["filament used [mm]"] ?: settings["total filament length [mm]"])
            .map { it.toDoubleOrNull()?.takeIf(Double::isFinite)?.coerceAtLeast(0.0)?.div(1000.0) ?: 0.0 }
            .padTo(toolCount)
        val rawGrams = csv(settings["filament used [g]"]).map { it.toDoubleOrNull()?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0 }
        val totalGrams = (settings["total filament used [g]"] ?: settings["total filament weight [g]"])?.toDoubleOrNull()?.takeIf(Double::isFinite) ?: 0.0
        val usedGrams = if (rawGrams.isNotEmpty()) rawGrams.padTo(toolCount) else {
            val sum = usedMeters.sum()
            (0 until toolCount).map { i -> if (sum > 0) totalGrams * usedMeters.getOrElse(i) { 0.0 } / sum else if (i == 0) totalGrams else 0.0 }
        }
        val filamentIds = csv(settings["filament_ids"]).padTo(toolCount, "")
        val maps = csv(settings["filament_map"]).padTo(toolCount, "1")
        val nozzle = csv(settings["nozzle_diameter"]).firstOrNull()?.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() } ?: 0.4
        if (sequence.isEmpty()) sequence.addAll((1..toolCount).toList())
        return PrintMetadata(
            settings, layers, seconds, firstLayer, nozzle, materials, colors, usedMeters,
            usedGrams, filamentIds, maps, sequence, objects.values.toList(),
        )
    }

    private fun readGcodeThumbnail(file: File): ByteArray? {
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.US_ASCII), 32 * 1024).use { reader ->
            var active = false
            var maxPixels = 0L
            var best: ByteArray? = null
            var expectedChars = 0
            val encoded = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                if (!active) {
                    val match = thumbnailBegin.matchEntire(line.trim()) ?: continue
                    active = true
                    expectedChars = match.groupValues[3].toIntOrNull() ?: 0
                    encoded.setLength(0)
                    continue
                }
                if (line.trim().startsWith("; thumbnail end", ignoreCase = true)) {
                    active = false
                    val candidate = try { android.util.Base64.decode(encoded.toString(), android.util.Base64.DEFAULT) } catch (_: IllegalArgumentException) { null }
                    if (candidate != null && isPng(candidate) && encoded.length >= expectedChars && candidate.size.toLong() > maxPixels) {
                        maxPixels = candidate.size.toLong()
                        best = candidate
                    }
                    continue
                }
                encoded.append(line.trim().removePrefix(";").trim())
            }
            return best
        }
    }

    private fun parseM73Remaining(file: File): Int {
        var remainingMinutes: Int? = null
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.US_ASCII), 32 * 1024).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                val match = Regex("(?i)^M73\\s+P(\\d+)\\s+R(\\d+)").find(line.trim()) ?: continue
                val percent = match.groupValues[1].toIntOrNull() ?: continue
                val minutes = match.groupValues[2].toIntOrNull() ?: continue
                if (percent <= 1) remainingMinutes = maxOf(remainingMinutes ?: 0, minutes)
            }
        }
        return (remainingMinutes ?: 0) * 60
    }

    private fun parseDuration(value: String): Int {
        var seconds = 0.0
        durationPart.findAll(value).forEach { match ->
            val amount = match.groupValues[1].toDoubleOrNull() ?: 0.0
            seconds += amount * when (match.groupValues[2].lowercase(Locale.ROOT)) {
                "d" -> 86400
                "h" -> 3600
                "m" -> 60
                else -> 1
            }
        }
        return seconds.toInt().coerceAtLeast(0)
    }

    private fun csv(value: String?): List<String> = value.orEmpty().split(',').map { it.trim().trim('"') }.filter { it.isNotEmpty() }

    private fun <T : Any> List<T>.padTo(size: Int, fallback: T? = null): List<T> {
        if (this.size >= size) return take(size)
        if (isEmpty()) return if (fallback == null) emptyList() else List(size) { fallback }
        return this + List(size - this.size) { fallback ?: last() }
    }

    private fun isPng(bytes: ByteArray): Boolean = bytes.size >= 24 &&
        bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) &&
        bytes.copyOfRange(12, 16).contentEquals(byteArrayOf(0x49, 0x48, 0x44, 0x52)) &&
        pngUInt32(bytes, 16) > 0L && pngUInt32(bytes, 20) > 0L

    private fun pngUInt32(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 0xff) shl 24) or
            ((bytes[offset + 1].toLong() and 0xff) shl 16) or
            ((bytes[offset + 2].toLong() and 0xff) shl 8) or
            (bytes[offset + 3].toLong() and 0xff)

    private fun digest(file: File): String = FileInputStream(file).use(::digest)

    private fun digest(input: java.io.InputStream): String {
        val md5 = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(128 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            md5.update(buffer, 0, count)
        }
        return md5.digest().joinToString("") { "%02X".format(Locale.ROOT, it) }
    }

    private fun writeText(zip: ZipOutputStream, path: String, contents: String) =
        writeBytes(zip, path, contents.toByteArray(StandardCharsets.UTF_8))

    private fun writeBytes(zip: ZipOutputStream, path: String, contents: ByteArray) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(contents)
        zip.closeEntry()
    }

    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private val contentTypesXml = """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
 <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
 <Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>
 <Default Extension="png" ContentType="image/png"/>
 <Default Extension="gcode" ContentType="text/x.gcode"/>
</Types>
"""

    private val rootRelationshipsXml = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
 <Relationship Target="/Metadata/plate_1.png" Id="rel-2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail"/>
</Relationships>
"""

    private fun modelXml(version: String, title: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        val safeTitle = xml(title)
        return """<?xml version="1.0" encoding="UTF-8"?>
<model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02" xmlns:QIDIStudio="http://schemas.qiditech.com/package/2021" xmlns:p="http://schemas.microsoft.com/3dmanufacturing/production/2015/06" requiredextensions="p">
 <metadata name="Application">OrkanSlicer-${xml(version)}</metadata>
 <metadata name="BambuStudio:3mfVersion">1</metadata>
 <metadata name="CreationDate">$date</metadata>
 <metadata name="ModificationDate">$date</metadata>
 <metadata name="Origin">gcode</metadata>
 <metadata name="ProfileTitle">$safeTitle</metadata>
 <metadata name="QIDIStudio:3mfVersion">1</metadata>
 <metadata name="Thumbnail_Middle">/Metadata/plate_1.png</metadata>
 <metadata name="Thumbnail_Small">/Metadata/plate_1.png</metadata>
 <metadata name="Title">$safeTitle</metadata>
 <resources/>
 <build/>
</model>
"""
    }

    private fun modelSettingsRelationshipsXml(gcodePath: String) = """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Target="/$gcodePath" Id="rel-1" Type="http://schemas.qiditech.com/package/2021/gcode"/>
</Relationships>
"""

    private fun modelSettingsXml(metadata: PrintMetadata): String {
        val maps = metadata.filamentMaps.joinToString(" ")
        val zeros = metadata.materials.joinToString(" ") { "0" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<config><plate>
 <metadata key="plater_id" value="$PLATE"/>
 <metadata key="plater_name" value=""/>
 <metadata key="locked" value="false"/>
 <metadata key="filament_map_mode" value="Auto For Flush"/>
 <metadata key="filament_maps" value="${xml(maps)}"/>
 <metadata key="filament_volume_maps" value="$zeros"/>
 <metadata key="gcode_file" value="Metadata/plate_$PLATE.gcode"/>
 <metadata key="thumbnail_file" value="Metadata/plate_$PLATE.png"/>
 <metadata key="thumbnail_no_light_file" value="Metadata/plate_no_light_$PLATE.png"/>
 <metadata key="top_file" value="Metadata/top_$PLATE.png"/>
 <metadata key="pick_file" value="Metadata/pick_$PLATE.png"/>
 <metadata key="pattern_bbox_file" value="Metadata/plate_$PLATE.json"/>
</plate></config>
"""
    }

    private fun sliceInfoXml(metadata: PrintMetadata, printer: ConfigObject, version: String): String {
        val modelId = xml(printer.get("printer_model")?.takeIf { it.isNotBlank() } ?: "Q2")
        val objectXml = metadata.objects.mapIndexed { index, item ->
            " <object identify_id=\"${index + 1}\" name=\"${xml(item.name)}\" skipped=\"false\"/>"
        }.joinToString("\n")
        val filaments = metadata.materials.indices.joinToString("\n") { i ->
            " <filament id=\"${i + 1}\" tray_info_idx=\"${xml(metadata.filamentIds.getOrElse(i) { "" })}\" " +
                "type=\"${xml(metadata.materials[i])}\" color=\"${xml(metadata.colors[i])}\" " +
                "used_m=\"${fmt(metadata.usedMeters.getOrElse(i) { 0.0 })}\" used_g=\"${fmt(metadata.usedGrams.getOrElse(i) { 0.0 })}\" " +
                "used_for_object=\"true\" used_for_support=\"false\" group_id=\"0\" nozzle_diameter=\"${fmt(metadata.nozzle)}\" volume_type=\"Standard\"/>"
        }
        val filamentList = metadata.materials.indices.joinToString(" ") { it.toString() }
        val layerEnd = (metadata.layers - 1).coerceAtLeast(0)
        val allObjects = if (objectXml.isEmpty()) "" else "\n$objectXml\n"
        return """<?xml version="1.0" encoding="UTF-8"?>
<config>
 <header>
  <header_item key="X-QDT-Client-Type" value="slicer"/>
  <header_item key="X-QDT-Client-Version" value="${xml(version)}"/>
 </header>
 <plate>
  <metadata key="index" value="$PLATE"/>
  <metadata key="extruder_type" value="0"/>
  <metadata key="nozzle_volume_type" value="0"/>
  <metadata key="printer_model_id" value="$modelId"/>
  <metadata key="nozzle_diameters" value="${fmt(metadata.nozzle)}"/>
  <metadata key="timelapse_type" value="${xml(metadata.settings["timelapse_type"] ?: "0")}"/>
  <metadata key="prediction" value="${metadata.seconds}"/>
  <metadata key="weight" value="${fmt(metadata.usedGrams.sum())}"/>
  <metadata key="first_layer_time" value="${fmt(metadata.firstLayerSeconds)}"/>
  <metadata key="outside" value="false"/>
  <metadata key="support_used" value="false"/>
  <metadata key="label_object_enabled" value="${metadata.objects.isNotEmpty()}"/>
  <metadata key="filament_maps" value="${xml(metadata.filamentMaps.joinToString(" "))}"/>
  <metadata key="limit_filament_maps" value="${metadata.materials.joinToString(" ") { "0" }}"/>$allObjects
$filaments
  <layer_filament_lists><layer_filament_list filament_list="$filamentList" layer_ranges="0 $layerEnd"/></layer_filament_lists>
 </plate>
</config>
"""
    }

    private fun plateJson(metadata: PrintMetadata): String {
        val bounds = metadata.objects
        val minX = bounds.minOfOrNull { it.minX } ?: 0.0
        val minY = bounds.minOfOrNull { it.minY } ?: 0.0
        val maxX = bounds.maxOfOrNull { it.maxX } ?: 0.0
        val maxY = bounds.maxOfOrNull { it.maxY } ?: 0.0
        val layerHeight = metadata.settings["layer_height"]?.toDoubleOrNull() ?: 0.2
        val objects = bounds.mapIndexed { index, item ->
            "{\"area\":${fmt(item.area)},\"bbox\":[${fmt(item.minX)},${fmt(item.minY)},${fmt(item.maxX)},${fmt(item.maxY)}]," +
                "\"id\":${index + 1},\"layer_height\":${fmt(layerHeight)},\"name\":${jsonQuote(item.name)}}"
        }.joinToString(",")
        val colors = metadata.colors.joinToString(",") { jsonQuote(it) }
        val filamentIds = metadata.materials.indices.joinToString(",")
        return "{\"bbox_all\":[${fmt(minX)},${fmt(minY)},${fmt(maxX)},${fmt(maxY)}]," +
            "\"bbox_objects\":[$objects],\"bed_type\":\"textured_plate\",\"filament_colors\":[$colors]," +
            "\"filament_ids\":[$filamentIds],\"first_extruder\":${(metadata.sequence.firstOrNull() ?: 1) - 1}," +
            "\"first_layer_time\":${fmt(metadata.firstLayerSeconds)},\"is_seq_print\":false," +
            "\"nozzle_diameter\":${fmt(metadata.nozzle)},\"version\":2}"
    }

    private fun settingsJson(metadata: PrintMetadata, printer: ConfigObject): String {
        val values = LinkedHashMap<String, String>()
        listOf("layer_height", "curr_bed_type", "filament_map", "filament_colour", "filament_type", "filament_ids", "nozzle_diameter", "timelapse_type")
            .forEach { key -> metadata.settings[key]?.let { values[key] = it } }
        values["printer_model"] = printer.get("printer_model") ?: "Q2"
        values["gcode_flavor"] = "klipper"
        return values.entries.joinToString(prefix = "{\n", postfix = "\n}\n", separator = ",\n") { (key, value) ->
            "  ${jsonQuote(key)}: ${jsonQuote(value)}"
        }
    }

    private fun sequenceJson(metadata: PrintMetadata): String {
        return "{\"plate_$PLATE\":{\"sequence\":[${metadata.sequence.joinToString(",")}]}}"
    }

    private fun fmt(value: Double): String = if (value.isFinite()) String.format(Locale.ROOT, "%.4f", value).trimEnd('0').trimEnd('.') else "0"

    private fun jsonQuote(value: String): String = buildString {
        append('"')
        for (character in value) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(Locale.ROOT, character.code)) else append(character)
            }
        }
        append('"')
    }
}
