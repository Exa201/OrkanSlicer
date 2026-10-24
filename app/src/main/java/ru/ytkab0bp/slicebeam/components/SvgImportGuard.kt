package ru.ytkab0bp.slicebeam.components

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.EntityResolver
import org.xml.sax.SAXException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Bounds SVG work before it reaches the native slicer importer. */
class SvgImportGuard private constructor() {
    companion object {
        private const val MAX_FILE_BYTES = 5L * 1024L * 1024L
        private const val MAX_SIMPLIFIABLE_BYTES = 25L * 1024L * 1024L
        private const val MAX_PATH_COMMANDS = 3500
        private val PATH_TOKEN = Pattern.compile(
            "[MmZzLlHhVvCcSsQqTtAa]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?\\d*)(?:[eE][-+]?\\d+)?"
        )

        /** Returns a user-readable problem, or null when the SVG is within safe import limits. */
        @JvmStatic
        @Throws(IOException::class)
        fun validate(file: File): String? {
            if (file.length() > MAX_FILE_BYTES) {
                return "Diese SVG-Datei ist zu groß für den mobilen Import (maximal 5 MB)."
            }
            val commands = countPathCommands(file)
            if (commands > MAX_PATH_COMMANDS) {
                return "Diese SVG enthält $commands Konturpunkte und ist für den mobilen Import zu detailliert."
            }
            return null
        }

        /** True when a large SVG is within the bounded size supported by the simplifier. */
        @JvmStatic
        fun canSimplify(file: File?): Boolean =
            file != null && file.isFile && file.length() > 0 && file.length() <= MAX_SIMPLIFIABLE_BYTES

        /** Simplifies dense straight-line SVG paths while preserving the SVG document and its styling. */
        @JvmStatic
        @Throws(IOException::class)
        fun simplify(source: File, destination: File): Int {
            val document: Document = try {
                FileInputStream(source).use { input ->
                    secureDocumentBuilder(source).parse(input)
                }
            } catch (error: Exception) {
                if (error is IOException) throw error
                if (error is ParserConfigurationException || error is SAXException || error is IllegalArgumentException) {
                    throw IOException("Die SVG-Datei ist beschädigt oder kann nicht sicher gelesen werden.", error)
                }
                throw error
            }

            var paths = document.getElementsByTagNameNS("*", "path")
            if (paths.length == 0) paths = document.getElementsByTagName("path")
            removeNonVisualMetadata(document.documentElement)
            var before = 0
            var after = 0
            val polygons = document.getElementsByTagNameNS("*", "polygon")
            val polylines = document.getElementsByTagNameNS("*", "polyline")
            for (index in 0 until paths.length) {
                before += countCommands((paths.item(index) as Element).getAttribute("d"))
            }
            before += countPoints(polygons) + countPoints(polylines)
            var tolerance = 0.35
            for (pass in 0 until 10) {
                after = 0
                for (index in 0 until paths.length) {
                    val path = paths.item(index) as Element
                    val data = path.getAttribute("d")
                    val reduced = simplifyPath(data, tolerance)
                    after += countCommands(reduced)
                    path.setAttribute("d", reduced)
                }
                after += simplifyPoints(polygons, tolerance) + simplifyPoints(polylines, tolerance)
                if (after <= MAX_PATH_COMMANDS) break
                tolerance *= 1.8
            }
            if (after > MAX_PATH_COMMANDS) {
                throw IOException("Die SVG enthält überwiegend Kurven oder andere nicht vereinfachbare Elemente. Bitte exportiere sie in einer gröberen Detailstufe.")
            }

            try {
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
                BufferedOutputStream(FileOutputStream(destination)).use { output ->
                    transformer.transform(DOMSource(document), StreamResult(output))
                }
            } catch (error: Exception) {
                destination.delete()
                throw IOException("Die vereinfachte SVG-Datei konnte nicht gespeichert werden.", error)
            }
            return before - after
        }

        @Throws(IOException::class)
        private fun countPathCommands(file: File): Int {
            try {
                FileInputStream(file).use { input ->
                    val document = secureDocumentBuilder(file).parse(input)
                    var root = document.documentElement.localName
                    if (root == null) root = document.documentElement.tagName
                    if (!"svg".equals(root, ignoreCase = true)) {
                        throw IOException("Die Datei enthält kein gültiges SVG-Bild.")
                    }
                    var paths = document.getElementsByTagNameNS("*", "path")
                    if (paths.length == 0) paths = document.getElementsByTagName("path")
                    var count = 0
                    for (index in 0 until paths.length) {
                        count += countCommands((paths.item(index) as Element).getAttribute("d"))
                    }
                    count += countPoints(document.getElementsByTagNameNS("*", "polygon"))
                    count += countPoints(document.getElementsByTagNameNS("*", "polyline"))
                    return count
                }
            } catch (error: IOException) {
                throw error
            } catch (error: Exception) {
                if (error is ParserConfigurationException || error is SAXException || error is IllegalArgumentException) {
                    throw IOException("Die Datei enthält kein lesbares SVG-Bild.", error)
                }
                throw error
            }
        }

        /**
         * Android's platform XML parser does not implement Xerces' disallow-doctype feature.
         * Keep the same fail-closed behavior there by rejecting DTD/entity declarations from the
         * bounded input stream before parsing; an entity resolver is also installed as a second
         * barrier. This lets ordinary SVG files pass validation on Android without enabling XXE.
         */
        @Throws(IOException::class, ParserConfigurationException::class)
        private fun secureDocumentBuilder(file: File): DocumentBuilder {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            var mustRejectDtdInInput = false
            for ((feature, value) in arrayOf(
                "http://apache.org/xml/features/disallow-doctype-decl" to true,
                "http://xml.org/sax/features/external-general-entities" to false,
                "http://xml.org/sax/features/external-parameter-entities" to false,
            )) {
                try {
                    factory.setFeature(feature, value)
                } catch (_: ParserConfigurationException) {
                    mustRejectDtdInInput = true
                }
            }
            factory.isExpandEntityReferences = false
            // XInclude is disabled by default. Android's platform factory throws
            // UnsupportedOperationException for setXIncludeAware(false), so don't call it.
            if (mustRejectDtdInInput) rejectDtdDeclarations(file)
            return factory.newDocumentBuilder().apply {
                setEntityResolver(EntityResolver { _, _ ->
                    throw SAXException("Externe XML-Entitäten sind deaktiviert.")
                })
            }
        }

        private fun rejectDtdDeclarations(file: File) {
            val markers = listOf("<!DOCTYPE", "<!ENTITY")
            var tail = ""
            BufferedInputStream(FileInputStream(file)).use { input ->
                val bytes = ByteArray(8192)
                while (true) {
                    val count = input.read(bytes)
                    if (count < 0) break
                    val text = (tail + String(bytes, 0, count, Charsets.US_ASCII)).uppercase(java.util.Locale.ROOT)
                    if (markers.any(text::contains)) {
                        throw IOException("Die Datei enthält kein lesbares SVG-Bild (DOCTYPE/ENTITY-Deklarationen sind nicht erlaubt).")
                    }
                    tail = text.takeLast(markers.maxOf(String::length) - 1)
                }
            }
        }

        private fun countPoints(nodes: NodeList): Int {
            var count = 0
            for (index in 0 until nodes.length) {
                val points = (nodes.item(index) as Element).getAttribute("points")
                val matcher = PATH_TOKEN.matcher(points)
                var numbers = 0
                while (matcher.find()) if (!isCommand(matcher.group()[0])) numbers++
                count += numbers / 2
            }
            return count
        }

        private fun simplifyPoints(nodes: NodeList, tolerance: Double): Int {
            var count = 0
            for (index in 0 until nodes.length) {
                val shape = nodes.item(index) as Element
                val points = shape.getAttribute("points")
                val matcher = PATH_TOKEN.matcher(points)
                val numbers = ArrayList<String>()
                while (matcher.find()) {
                    val token = matcher.group()
                    if (!isCommand(token[0])) numbers.add(token)
                }
                if (numbers.size < 4 || numbers.size % 2 != 0) {
                    count += numbers.size / 2
                    continue
                }
                val reduced = StringBuilder(points.length)
                var keptX = parse(numbers[0])
                var keptY = parse(numbers[1])
                reduced.append(numbers[0]).append(',').append(numbers[1])
                var kept = 1
                var point = 2
                while (point < numbers.size) {
                    val x = parse(numbers[point])
                    val y = parse(numbers[point + 1])
                    val last = point + 2 == numbers.size
                    if (last || distance(x, y, keptX, keptY) >= tolerance) {
                        reduced.append(' ').append(numbers[point]).append(',').append(numbers[point + 1])
                        keptX = x
                        keptY = y
                        kept++
                    }
                    point += 2
                }
                shape.setAttribute("points", reduced.toString())
                count += kept
            }
            return count
        }

        private fun countCommands(data: String): Int {
            val matcher = PATH_TOKEN.matcher(data)
            val tokens = ArrayList<String>()
            while (matcher.find()) tokens.add(matcher.group())
            var count = 0
            var command = 0.toChar()
            var index = 0
            while (index < tokens.size) {
                val token = tokens[index]
                if (token.length == 1 && isCommand(token[0])) {
                    command = token[0]
                    if (command.uppercaseChar() == 'Z') count++
                    index++
                    continue
                }
                val arity = parameterCount(command)
                if (arity == 0 || index + arity > tokens.size) {
                    index++
                    continue
                }
                var group = true
                for (position in 0 until arity) {
                    val value = tokens[index + position]
                    if (value.length == 1 && isCommand(value[0])) {
                        group = false
                        break
                    }
                }
                if (!group) {
                    index++
                    continue
                }
                count++
                index += arity
                if (command.uppercaseChar() == 'M') command = if (command.isLowerCase()) 'l' else 'L'
            }
            return count
        }

        private fun removeNonVisualMetadata(parent: Node) {
            var child = parent.firstChild
            while (child != null) {
                val next = child.nextSibling
                val name = child.localName
                if (child.nodeType == Node.COMMENT_NODE || name == "metadata" || name == "title" || name == "desc") {
                    parent.removeChild(child)
                } else {
                    removeNonVisualMetadata(child)
                }
                child = next
            }
        }

        private fun parameterCount(command: Char): Int = when (command.uppercaseChar()) {
            'H', 'V' -> 1
            'M', 'L', 'T' -> 2
            'S', 'Q' -> 4
            'C' -> 6
            'A' -> 7
            else -> 0
        }

        private fun isCommand(char: Char): Boolean = "MmZzLlHhVvCcSsQqTtAa".indexOf(char) >= 0

        private fun simplifyPath(data: String, tolerance: Double): String {
            val matcher = PATH_TOKEN.matcher(data)
            val tokens = ArrayList<String>()
            while (matcher.find()) tokens.add(matcher.group())
            val output = StringBuilder(data.length)
            var command = 0.toChar()
            var lastX = Double.NaN
            var lastY = Double.NaN
            var keptX = Double.NaN
            var keptY = Double.NaN
            var index = 0
            var firstMove = false
            while (index < tokens.size) {
                val token = tokens[index]
                if (token.length == 1 && isCommand(token[0])) {
                    command = token[0]
                    firstMove = command == 'M' || command == 'm'
                    output.append(command)
                    index++
                    if (command == 'Z' || command == 'z') {
                        lastX = Double.NaN
                        keptX = Double.NaN
                        lastY = Double.NaN
                        keptY = Double.NaN
                    }
                    continue
                }
                if (command == 0.toChar()) {
                    output.append(token).append(' ')
                    index++
                    continue
                }
                val upper = command.uppercaseChar()
                val arity = when (upper) {
                    'H', 'V' -> 1
                    'M', 'L', 'T' -> 2
                    'S', 'Q' -> 4
                    'C' -> 6
                    'A' -> 7
                    else -> 0
                }
                if (arity == 0 || index + arity > tokens.size) {
                    output.append(token).append(' ')
                    index++
                    continue
                }
                var numericGroup = true
                for (position in 0 until arity) {
                    val value = tokens[index + position]
                    if (value.length == 1 && isCommand(value[0])) {
                        numericGroup = false
                        break
                    }
                }
                if (!numericGroup) {
                    index++
                    continue
                }
                val values = DoubleArray(arity) { parse(tokens[index + it]) }
                val move = upper == 'M' && firstMove
                if (upper == 'M') firstMove = false
                val line = upper == 'L' || upper == 'H' || upper == 'V' || (upper == 'M' && !move)
                var x = lastX
                var y = lastY
                if (upper == 'H') x = if (isRelative(command)) lastX + values[0] else values[0]
                else if (upper == 'V') y = if (isRelative(command)) lastY + values[0] else values[0]
                else if (arity >= 2) {
                    val endpoint = when (upper) {
                        'C' -> 4
                        'S', 'Q' -> 2
                        'A' -> 5
                        else -> 0
                    }
                    x = if (isRelative(command)) lastX + values[endpoint] else values[endpoint]
                    y = if (isRelative(command)) lastY + values[endpoint + 1] else values[endpoint + 1]
                }
                var keep = !line || move || !keptX.isFinite() || !lastX.isFinite() || distance(x, y, keptX, keptY) >= tolerance
                // Keep the endpoint of a line run, but allow line-to-line detail to be removed.
                val atEnd = index + arity >= tokens.size
                val nextIsCommand = !atEnd && tokens[index + arity].length == 1 && isCommand(tokens[index + arity][0])
                val nextIsLine = nextIsCommand && "MmLlHhVv".indexOf(tokens[index + arity][0]) >= 0
                if (line && (atEnd || (nextIsCommand && !nextIsLine))) keep = true
                if (keep) {
                    if (line && !move && isRelative(command) && keptX.isFinite() && keptY.isFinite()) {
                        when (upper) {
                            'H' -> output.append(format(x - keptX)).append(' ')
                            'V' -> output.append(format(y - keptY)).append(' ')
                            else -> output.append(format(x - keptX)).append(' ').append(format(y - keptY)).append(' ')
                        }
                    } else {
                        for (position in 0 until arity) output.append(tokens[index + position]).append(' ')
                    }
                    keptX = x
                    keptY = y
                }
                lastX = x
                lastY = y
                index += arity
                // Additional coordinate pairs after M are implicit L commands.
                if (upper == 'M') command = if (command.isLowerCase()) 'l' else 'L'
            }
            return output.toString().trim()
        }

        private fun isRelative(command: Char): Boolean = command.isLowerCase()
        private fun parse(value: String): Double = value.toDoubleOrNull() ?: Double.NaN
        private fun format(value: Double): String = value.toString()
        private fun distance(x: Double, y: Double, otherX: Double, otherY: Double): Double {
            val dx = x - otherX
            val dy = y - otherY
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
}
