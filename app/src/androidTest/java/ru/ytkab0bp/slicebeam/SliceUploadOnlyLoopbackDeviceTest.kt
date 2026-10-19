package ru.ytkab0bp.slicebeam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceUploadController
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.GCodeIntegrityValidator
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real post-slice upload-only path against a loopback server; no printer is contacted. */
@RunWith(AndroidJUnit4::class)
class SliceUploadOnlyLoopbackDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun workspaceUploadOnlyUploadsSlicedGcodeToLoopbackWithoutPrintStartRequest() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val config = SliceBeam.requireConfig()
        val printer = requireNotNull(config.findPrinter("Anycubic 4Max Pro 0.4 nozzle"))
        val process = requireNotNull(config.presets?.get("print"))
        val filament = requireNotNull(config.presets?.get("filament"))
        val modelFile = File(activity.cacheDir, "upload-only-loopback-${System.nanoTime()}.stl")
        val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 120_000 }
        val host = "http://127.0.0.1:${server.localPort}"
        printer.put("host_type", "moonraker")
        printer.put("print_host", host)
        printer.put("printhost_apikey", "")
        // This generic Marlin fixture uses relative E; the bundled profile leaves the
        // per-layer reset empty, which the native slicer correctly rejects.
        printer.put("layer_gcode", "G92 E0")
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process)
            put("filament", filament)
        }

        val requestPaths = mutableListOf<String>()
        val requestBodies = mutableListOf<String>()
        val serverFailure = AtomicReference<Throwable?>()
        val expectedGcodeName = AtomicReference<String>()
        val requestsHandled = CountDownLatch(1)
        val serverThread = Thread {
            try {
                var requestIndex = 0
                while (requestIndex < 2) {
                    val socket = try { server.accept() } catch (_: java.net.SocketTimeoutException) { break }
                    socket.use {
                        it.soTimeout = 15_000
                        val input = it.getInputStream()
                        val requestLine = readHttpLine(input)
                        val headers = linkedMapOf<String, String>()
                        while (true) {
                            val line = readHttpLine(input)
                            if (line.isEmpty()) break
                            val separator = line.indexOf(':')
                            if (separator > 0) headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
                        }
                        synchronized(requestPaths) { requestPaths += requestLine }
                        val body = readHttpBody(input, headers)
                        synchronized(requestPaths) {
                            requestBodies += body.toString(StandardCharsets.ISO_8859_1)
                        }
                        val responseBody = JSONObject().put(
                            "result",
                            JSONObject().put("item", JSONObject().put("root", "gcodes").put("path", expectedGcodeName.get()))
                                .put("action", "create_file"),
                        ).toString().toByteArray(StandardCharsets.UTF_8)
                        val output = it.getOutputStream()
                        output.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseBody.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(responseBody)
                        output.flush()
                    }
                    requestIndex++
                    server.soTimeout = 1_500
                }
            } catch (error: Throwable) {
                serverFailure.set(error)
            } finally {
                requestsHandled.countDown()
                runCatching { server.close() }
            }
        }.apply { isDaemon = true; name = "OrkanSlicer-upload-only-loopback"; start() }

        try {
            activity.assets.open("models/xyz_cube.stl").use { source -> modelFile.outputStream().use(source::copyTo) }
            val loaded = CountDownLatch(1)
            val loadedModel = AtomicReference<Model?>()
            val loadFailure = AtomicReference<Exception?>()
            bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModelValue: Model, firstNewObject: Int, addedObjects: Int) {
                    loadedModel.set(loadedModelValue)
                    loaded.countDown()
                }
                override fun onFailed(error: Exception) {
                    loadFailure.set(error)
                    loaded.countDown()
                }
            })
            assertTrue("Disposable cube should load", loaded.await(60, TimeUnit.SECONDS))
            assertEquals("Model load failure: ${loadFailure.get()}", 1, requireNotNull(loadedModel.get()).objectsCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlice)).assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuSliceCurrentSettings)).assertIsDisplayed().performClick()
            compose.onNodeWithTag("pre-slice-confirm").assertIsDisplayed().performClick()
            val mismatchContinue = compose.onAllNodesWithText(activity.getString(R.string.PreSliceMaterialMismatchContinue))
            if (mismatchContinue.fetchSemanticsNodes().isNotEmpty()) mismatchContinue.get(0).performClick()
            compose.waitUntil(timeoutMillis = 120_000) { bed.hasSliceResult() && bed.glView?.renderer?.gcodeResult != null }
            assertTrue("Native slice must produce a real G-code file", BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0)
            val gcodeIntegrity = GCodeIntegrityValidator.validate(BedFragment.tempGCodePath)
            assertTrue("Generated G-code must be valid before upload: ${gcodeIntegrity.details}", gcodeIntegrity.isValid())
            expectedGcodeName.set(requireNotNull(bed.glView?.renderer?.gcodeResult).getRecommendedName())

            activity.runOnUiThread {
                SliceUploadController.upload(bed, "moonraker", host, null, false, printer, intArrayOf(0))
            }
            compose.waitUntil(timeoutMillis = 10_000) { bed.isPrinterUploadProgressAttached() }
            compose.onNodeWithTag("printer-upload-progress-title").assertIsDisplayed()
            compose.waitUntil(timeoutMillis = 20_000) {
                synchronized(requestPaths) { requestPaths.isNotEmpty() } || !bed.isPrinterUploadProgressAttached()
            }
            if (synchronized(requestPaths) { requestPaths.isEmpty() }) {
                val visibleText = compose.onAllNodesWithText("", substring = true).fetchSemanticsNodes()
                    .joinToString("\n") { it.config.toString() }
                throw AssertionError(
                    "Upload ended without a loopback request; server failure=${serverFailure.get()}; " +
                        "progress attached=${bed.isPrinterUploadProgressAttached()}; visible text=$visibleText",
                )
            }
            compose.waitUntil(timeoutMillis = 20_000) { !bed.isPrinterUploadProgressAttached() }
            assertTrue("Loopback server should complete the request observation", requestsHandled.await(3, TimeUnit.SECONDS))
            serverFailure.get()?.let { throw AssertionError("Loopback upload server failed", it) }
            synchronized(requestPaths) {
                assertEquals("Upload-only should issue exactly one request and no print-start request", 1, requestPaths.size)
                assertTrue("Moonraker upload endpoint is used", requestPaths.single().startsWith("POST /server/files/upload HTTP/"))
                assertTrue("The request contains the generated G-code filename", requestBodies.single().contains(
                    expectedGcodeName.get(),
                ))
            }
            assertTrue("Upload-only must preserve the sliced result", bed.hasSliceResult())

            // The mock server is closed after the successful request. A second upload must
            // present a failure dialog instead of crashing when the HTTP library supplies
            // null response headers for connection-refused failures.
            activity.runOnUiThread {
                SliceUploadController.upload(bed, "moonraker", host, null, false, printer, intArrayOf(0))
            }
            compose.waitUntil(timeoutMillis = 10_000) { bed.isPrinterUploadProgressAttached() }
            compose.waitUntil(timeoutMillis = 20_000) {
                !bed.isPrinterUploadProgressAttached() && bed.isWorkspaceMessageDialogAttached()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuSliceSendToPrinterFailed))
                .assertIsDisplayed()
            assertTrue("Upload connection failures must keep the workspace alive", !activity.isFinishing)
        } finally {
            runCatching { server.close() }
            serverThread.join(2_000)
            modelFile.delete()
        }
    }

    private fun readHttpLine(input: InputStream): String {
        val bytes = ByteArrayOutputStream()
        while (true) {
            val next = input.read()
            if (next < 0 || next == '\n'.code) break
            if (next != '\r'.code) bytes.write(next)
        }
        return bytes.toString("US-ASCII")
    }

    private fun readHttpBody(input: InputStream, headers: Map<String, String>): ByteArray {
        val body = ByteArrayOutputStream()
        val contentLength = headers["content-length"]?.toLongOrNull()
        if (contentLength != null) {
            var remaining = contentLength
            val buffer = ByteArray(8 * 1024)
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) throw java.io.EOFException("Upload body ended early")
                body.write(buffer, 0, read)
                remaining -= read
            }
        } else if (headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true) {
            while (true) {
                val size = readHttpLine(input).substringBefore(';').trim().toInt(16)
                if (size == 0) {
                    while (readHttpLine(input).isNotEmpty()) Unit
                    break
                }
                var remaining = size
                val buffer = ByteArray(8 * 1024)
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read < 0) throw java.io.EOFException("Chunked upload body ended early")
                    body.write(buffer, 0, read)
                    remaining -= read
                }
                readHttpLine(input)
            }
        }
        return body.toByteArray()
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()
        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config
        createAndroidComposeRule<MainActivity>()
    }
}
