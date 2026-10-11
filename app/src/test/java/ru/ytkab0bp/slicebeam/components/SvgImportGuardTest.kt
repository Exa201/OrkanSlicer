package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SvgImportGuardTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun simplifiesDenseLinesPreservingVisualsAndRemovingMetadata() {
        val densePoints = buildString {
            append("M0 0 ")
            for (point in 1..4000) append("L${point / 10.0} 0 ")
        }
        val source = svg(
            "dense.svg",
            "<title>preview text</title><!-- comment --><path style=\"stroke:black\" d=\"$densePoints\"/>"
        )
        val destination = File(temporaryFolder.root, "simplified.svg")

        assertNotNull(SvgImportGuard.validate(source))
        assertTrue(SvgImportGuard.canSimplify(source))
        val removed = SvgImportGuard.simplify(source, destination)

        assertTrue("simplification should remove many contour points", removed > 0)
        assertNull(SvgImportGuard.validate(destination))
        val output = destination.readText()
        assertTrue(output.contains("stroke:black"))
        assertFalse(output.contains("preview text"))
        assertFalse(output.contains("<!-- comment -->"))
    }

    @Test
    fun rejectsInvalidAndUnsafeSvgDocuments() {
        val notSvg = File(temporaryFolder.root, "not-svg.svg").apply {
            writeText("<html><path d=\"M0 0 L1 1\"/></html>")
        }
        assertTrue(expectIOException { SvgImportGuard.validate(notSvg) }.message!!.contains("kein gültiges SVG"))

        val doctype = File(temporaryFolder.root, "doctype.svg").apply {
            writeText(
                "<!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>" +
                    "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>&x;</text></svg>"
            )
        }
        assertTrue(expectIOException { SvgImportGuard.validate(doctype) }.message!!.contains("lesbares SVG"))
    }

    @Test
    fun enforcesImportAndSimplifierSizeBoundaries() {
        val oversized = File(temporaryFolder.root, "oversized.svg")
        FileOutputStream(oversized).use { it.write(ByteArray(5 * 1024 * 1024 + 1)) }

        assertTrue(SvgImportGuard.validate(oversized)!!.contains("maximal 5 MB"))
        assertTrue("the simplifier has a separate, higher input cap", SvgImportGuard.canSimplify(oversized))
        assertFalse(SvgImportGuard.canSimplify(null))
        assertFalse(SvgImportGuard.canSimplify(temporaryFolder.root))
    }

    @Test
    fun reportsDenseCurvesThatCannotBeSimplifiedSafely() {
        val curves = buildString {
            append("M0 0 ")
            repeat(3501) { append("C1 1 2 2 3 3 ") }
        }
        val source = svg("curves.svg", "<path d=\"$curves\"/>")
        val error = expectIOException {
            SvgImportGuard.simplify(source, File(temporaryFolder.root, "curves-output.svg"))
        }
        assertTrue(error.message!!.contains("überwiegend Kurven"))
    }

    private fun svg(name: String, contents: String): File {
        return File(temporaryFolder.root, name).apply {
            writeText("<svg xmlns=\"http://www.w3.org/2000/svg\">$contents</svg>")
        }
    }

    private fun expectIOException(action: () -> Unit): IOException {
        try {
            action()
        } catch (error: IOException) {
            return error
        }
        throw AssertionError("Expected IOException")
    }
}
