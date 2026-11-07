package ru.ytkab0bp.slicebeam

import android.app.Application
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.PrinterConfigFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the real workspace entry when the selected printer has no web host configured. */
@RunWith(AndroidJUnit4::class)
class PrinterWebWorkspaceRouteDeviceTest {
    private var previousConfig: Slic3rConfigWrapper? = null
    private var previousPreferences: Map<String, Any?> = emptyMap()

    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun missingHostDialogCancelBackAndSetupActionStayOnExpectedRoutes() {
        val activity = compose.activity
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val navigation = activity.navigationDelegate as MobileNavigationDelegate
        val configureButton = context.getString(R.string.PrinterWebConfigureDescription)
        val setupTitle = context.getString(R.string.PrinterWebSetupTitle)
        val setupMessage = context.getString(R.string.PrinterWebSetupMessage)

        assertTrue("The workspace is the initial route", navigation.getCurrentFragment() is BedFragment)

        // Cancel dismisses only the Compose dialog and leaves the GL workspace in place.
        compose.onNodeWithContentDescription(configureButton).assertIsDisplayed().performClick()
        compose.onNodeWithText(setupTitle).assertIsDisplayed()
        compose.onNodeWithText(setupMessage).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.waitUntil(10_000) { navigation.getCurrentFragment() is BedFragment }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(setupTitle).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription(configureButton).assertIsDisplayed()

        // Android Back uses BedFragment's active-overlay dismissal path.
        compose.onNodeWithContentDescription(configureButton).performClick()
        compose.onNodeWithText(setupTitle).assertIsDisplayed()
        compose.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { navigation.getCurrentFragment() is BedFragment }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(setupTitle).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription(configureButton).assertIsDisplayed()

        // Confirm follows the intended setup route and never opens a printer connection.
        compose.onNodeWithContentDescription(configureButton).performClick()
        compose.onNodeWithText(context.getString(R.string.PrinterWebSetupAction)).performClick()
        compose.waitUntil(10_000) { navigation.getCurrentFragment() is PrinterConfigFragment }
        compose.onNodeWithContentDescription(context.getString(R.string.SlotPrinterConfig)).assertIsDisplayed()
    }

    @Test
    fun configuredWorkspaceWebPanelLoadsLocallyAndBackDestroysItsWebView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val navigation = compose.activity.navigationDelegate as MobileNavigationDelegate
        val bed = navigation.getCurrentFragment() as BedFragment
        val server = ServerSocket(0).apply { soTimeout = 10_000 }
        val responseSent = CountDownLatch(1)
        val requestLine = AtomicReference<String?>()
        val serverFailure = AtomicReference<Throwable?>()
        val serverThread = Thread {
            try {
                server.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    requestLine.set(reader.readLine())
                    while (!reader.readLine().isNullOrEmpty()) { /* consume request headers */ }
                    val body = "<!doctype html><title>Orkan test dashboard</title>".toByteArray(StandardCharsets.UTF_8)
                    val output = socket.getOutputStream()
                    output.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                    output.write(body)
                    output.flush()
                }
            } catch (failure: Throwable) {
                serverFailure.set(failure)
            } finally {
                responseSent.countDown()
            }
        }.apply { name = "OrkanSlicer-local-printer-dashboard-test"; start() }

        try {
            val printer = requireNotNull(SliceBeam.requireConfig().findPrinter("Anycubic Kobra 3 0.4 nozzle"))
            printer.put("print_host", "http://127.0.0.1:${server.localPort}/")
            compose.runOnUiThread {
                bed.javaClass.getDeclaredMethod("updatePrinterWebViewAvailability")
                    .apply { isAccessible = true }
                    .invoke(bed)
            }

            compose.onNodeWithContentDescription(context.getString(R.string.PrinterOpenWebDescription))
                .assertIsDisplayed().performClick()
            assertTrue("The in-device test dashboard should receive the WebView request", responseSent.await(10, TimeUnit.SECONDS))
            serverFailure.get()?.let { throw AssertionError("Local dashboard server failed", it) }
            assertTrue("WebView should request the configured dashboard root", requestLine.get()?.startsWith("GET / HTTP/1.") == true)

            val controller = bed.javaClass.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            val activePanel = controller.javaClass.getMethod("getActivePanel")
            val webViewField = bed.javaClass.getDeclaredField("panelWebView").apply { isAccessible = true }
            val webView = compose.runOnIdle {
                assertTrue("Configured host opens the printer-web workspace panel",
                    activePanel.invoke(controller)?.toString() == "PRINTER_WEB")
                requireNotNull(webViewField.get(bed) as? WebView)
            }
            compose.runOnIdle { assertTrue("The technical WebView is attached while its Compose panel is active", webView.parent != null) }

            compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.runOnIdle {
                assertTrue("Back clears the printer-web panel", activePanel.invoke(controller) == null)
                assertTrue("Back destroys and detaches the technical WebView", webViewField.get(bed) == null && webView.parent == null)
            }
        } finally {
            runCatching { server.close() }
            serverThread.join(2_000)
        }
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        val preferences = requireNotNull(Prefs.getPrefs())
        previousConfig = SliceBeam.CONFIG
        previousPreferences = preferences.all.filterKeys {
            it == "crash" || it == "converter_storage_first_prompt" || it == "last_commit"
        }
        preferences.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        printer.put("print_host", "")
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

    @After
    fun restoreApplicationState() {
        SliceBeam.CONFIG = previousConfig
        val preferences = Prefs.getPrefs() ?: return
        val editor = preferences.edit()
            .remove("crash")
            .remove("converter_storage_first_prompt")
            .remove("last_commit")
        previousPreferences.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.commit()
    }
}
