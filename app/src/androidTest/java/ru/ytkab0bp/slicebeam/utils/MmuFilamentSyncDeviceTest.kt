package ru.ytkab0bp.slicebeam.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef.ConfigOptionType
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Runs real HTTP callbacks and Android JSON parsing in the isolated .slicetest application. */
@RunWith(AndroidJUnit4::class)
class MmuFilamentSyncDeviceTest {
    private val saved = LinkedHashMap<String, String?>()
    private val keys = listOf("filament_palette", "mmu_synced_printer", "mmu_snapshot_printer", "mmu_snapshot", "mmu_choice_printer", "mmu_choice_gates")
    private val title = "MMU sync test fixture"

    @Before fun savePreferences() {
        TemporaryProjectSettings.clear()
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        Prefs.init(application)
        val prefs = requireNotNull(Prefs.getPrefs())
        keys.forEach { saved[it] = prefs.getString(it, null) }
    }

    @After fun restorePreferences() {
        TemporaryProjectSettings.clear()
        val editor = requireNotNull(Prefs.getPrefs()).edit()
        saved.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
        editor.commit()
    }

    @Test fun parserAcceptsOneTwoAndEightTools() {
        for (count in listOf(1, 2, 8)) {
            val result = MmuFilamentSync.fromMoonraker(response(IntArray(count) { it }, IntArray(count) { 1 }))
            assertEquals(count, result.size)
            assertEquals("PLA", result[0]?.type)
            assertEquals(0xFF112233.toInt(), result[0]?.color)
        }
    }

    @Test fun parserDoesNotInventFilamentsForEmptyUnknownOrMalformedSlots() {
        val payload = response(intArrayOf(0, 1, 2, 3), intArrayOf(1, 0, 1, 1))
        val mmu = payload.getJSONObject("result").getJSONObject("status").getJSONObject("mmu")
        mmu.getJSONArray("gate_material").put(2, "unknown")
        mmu.getJSONArray("gate_color").put(3, "123456ZZ")
        val result = MmuFilamentSync.fromMoonraker(payload)
        assertNotNull(result[0])
        assertNull(result[1])
        assertNull(result[2])
        assertNull(result[3])
    }

    @Test(expected = JSONException::class) fun disabledMmuIsRejected() {
        val payload = response(intArrayOf(0), intArrayOf(1))
        payload.getJSONObject("result").getJSONObject("status").getJSONObject("mmu").put("enabled", false)
        MmuFilamentSync.fromMoonraker(payload)
    }

    @Test fun fullSyncExpandsShrinksAndRefreshesTheAssignedPhysicalGates() {
        Prefs.setFilamentSlots(listOf(FilamentSlot(0, "ABS")))
        assertEquals(4 to null, sync(response(intArrayOf(0, 1, 2, 3), intArrayOf(1, 1, 1, 1))))
        assertEquals(4, Prefs.getFilamentSlots().size)

        val gapPayload = response(intArrayOf(2, 0, 3, 1), intArrayOf(1, 0, 1, 0))
        assertEquals(2 to null, sync(gapPayload))
        assertEquals(listOf("PLA", "PETG"), Prefs.getFilamentSlots().map { it.type })
        assertArrayEquals(intArrayOf(0, 2), MmuFilamentSync.getSelectedGates(title, 2))
        assertArrayEquals(intArrayOf(0), MmuFilamentSync.getSelectedGates(title, 1))
        assertArrayEquals(intArrayOf(0, 2), MmuFilamentSync.loadedGates(title))
        assertEquals(1, MmuFilamentSync.toolForGate(title, 0))
        assertEquals(0, MmuFilamentSync.toolForGate(title, 2))

        gapPayload.getJSONObject("result").getJSONObject("status").getJSONObject("mmu").getJSONArray("gate_color").put(2, "ABCDEF")
        assertEquals(2 to null, sync(gapPayload, full = false))
        assertEquals(2, Prefs.getFilamentSlots().size)
        assertEquals(0xFFABCDEF.toInt(), Prefs.getFilamentSlots()[1].color)

        assertEquals(1 to null, sync(response(intArrayOf(0, 1, 2, 3), intArrayOf(0, 0, 0, 1))))
        assertEquals(1, Prefs.getFilamentSlots().size)
        assertEquals("TPU", Prefs.getFilamentSlots()[0].type)
        assertArrayEquals(intArrayOf(3), MmuFilamentSync.getSelectedGates(title, 1))
    }

    @Test fun automaticRefreshKeepsTheUsersSingleFilamentCount() {
        requireNotNull(Prefs.getPrefs()).edit().remove("mmu_choice_gates").commit()
        Prefs.setFilamentSlots(listOf(FilamentSlot(0, "ABS")))
        assertEquals(1 to null, sync(response(intArrayOf(0, 1, 2, 3), intArrayOf(1, 1, 1, 1)), full = false))
        assertEquals(1, Prefs.getFilamentSlots().size)
        assertEquals("PLA", Prefs.getFilamentSlots()[0].type)
        assertEquals(0xFF112233.toInt(), Prefs.getFilamentSlots()[0].color)
    }

    @Test fun deletingAMaterialKeepsTheRemainingPhysicalSlotsAligned() {
        MmuFilamentSync.setSelectedGates(title, intArrayOf(0, 2, 3))
        MmuFilamentSync.removeSelectedGate(0, 3)
        assertArrayEquals(intArrayOf(2, 3), MmuFilamentSync.getSelectedGates(title, 2))
        assertArrayEquals(intArrayOf(2), MmuFilamentSync.getSelectedGates(title, 1))
        assertNull(MmuFilamentSync.getSelectedGates(title, 3))
    }

    @Test fun disabledMmuReturnsAReadableExplanationAndPreservesThePalette() {
        Prefs.setFilamentSlots(listOf(FilamentSlot(0x123456, "ABS")))
        val payload = response(intArrayOf(0), intArrayOf(1))
        payload.getJSONObject("result").getJSONObject("status").getJSONObject("mmu").put("enabled", false)
        assertEquals(SliceBeam.requireInstance().getString(R.string.MmuSyncNotAvailable), sync(payload).second)
        assertEquals("ABS", Prefs.getFilamentSlots().single().type)
    }

    @Test fun incompleteAndEmptyPrinterResponsesLeaveThePreviousPaletteAndMappingIntact() {
        Prefs.setFilamentSlots(listOf(FilamentSlot(0x123456, "ABS")))
        MmuFilamentSync.setSelectedGates(title, intArrayOf(3))
        val invalid = response(intArrayOf(0), intArrayOf(1))
        invalid.getJSONObject("result").getJSONObject("status").getJSONObject("mmu").getJSONArray("gate_material").put(0, "unknown")
        assertNotNull(sync(invalid).second)
        assertNotNull(sync(response(intArrayOf(0), intArrayOf(0))).second)
        assertEquals(1, Prefs.getFilamentSlots().size)
        assertEquals("ABS", Prefs.getFilamentSlots()[0].type)
        assertEquals(0xFF123456.toInt(), Prefs.getFilamentSlots()[0].color)
        assertArrayEquals(intArrayOf(3), MmuFilamentSync.getSelectedGates(title, 1))
    }

    @Test fun projectAutoSyncPreservesFileColorsAndManualSyncOnlyChangesTemporaryPalette() {
        Prefs.setFilamentSlots(listOf(FilamentSlot(0xFF123456.toInt(), "ABS")))
        MmuFilamentSync.setSelectedGates(title, intArrayOf(3))
        val savedPalette = Prefs.getPrefs()!!.getString("filament_palette", null)
        TemporaryProjectSettings.activate(ThreeMfProjectSettings("project.3mf", emptyMap(), listOf(
            mapOf("filament_colour" to "#0000FF", "filament_type" to "PLA"),
            mapOf("filament_colour" to "#FF0000", "filament_type" to "PETG"),
        ), mapOf("filament_colour" to ConfigOptionType.STRINGS, "filament_type" to ConfigOptionType.STRINGS)))
        assertNull(sync(response(intArrayOf(0, 1, 2, 3), intArrayOf(1, 1, 1, 1)), full = false).second)
        assertEquals(2, Prefs.getFilamentSlots().size)
        assertEquals(0xFF0000FF.toInt(), Prefs.getFilamentSlots()[0].color)
        assertNull(sync(response(intArrayOf(0, 1, 2, 3), intArrayOf(1, 1, 1, 1)), full = true).second)
        assertEquals(4, Prefs.getFilamentSlots().size)
        assertEquals(savedPalette, Prefs.getPrefs()!!.getString("filament_palette", null))
        assertEquals("3", Prefs.getPrefs()!!.getString("mmu_choice_gates", null))
        TemporaryProjectSettings.clear()
        assertEquals("ABS", Prefs.getFilamentSlots().single().type)
        assertArrayEquals(intArrayOf(3), MmuFilamentSync.getSelectedGates(title, 1))
    }

    private fun response(mapping: IntArray, status: IntArray): JSONObject {
        val materials = listOf("PLA", "ABS", "PETG", "TPU")
        val colors = listOf("#112233", "445566", "778899FF", "AABBCC")
        val mmu = JSONObject().put("enabled", true).put("ttg_map", JSONArray(mapping.toList()))
            .put("gate_status", JSONArray(status.toList()))
            .put("gate_material", JSONArray(status.indices.map { materials[it % 4] }))
            .put("gate_color", JSONArray(status.indices.map { colors[it % 4] }))
        return JSONObject().put("result", JSONObject().put("status", JSONObject().put("mmu", mmu)))
    }

    private fun sync(response: JSONObject, full: Boolean = true): Pair<Int, String?> {
        val body = response.toString().toByteArray(Charsets.UTF_8)
        val failure = AtomicReference<Throwable?>()
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 10_000
            val worker = Thread {
                try {
                    server.accept().use { socket ->
                        socket.soTimeout = 10_000
                        val reader = socket.getInputStream().bufferedReader()
                        assertTrue(reader.readLine().startsWith("GET /printer/objects/query?mmu "))
                        while (!reader.readLine().isNullOrEmpty()) { }
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(body)
                            flush()
                        }
                    }
                } catch (error: Throwable) { failure.set(error) }
            }.apply { start() }
            val completed = CountDownLatch(1)
            val result = AtomicReference<Pair<Int, String?>>()
            val printer = ConfigObject(title).apply { put("print_host", "http://127.0.0.1:${server.localPort}") }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val callback = MmuFilamentSync.Callback { updated, error ->
                    result.set(updated to error)
                    completed.countDown()
                }
                if (full) MmuFilamentSync.synchronizeAll(printer, callback) else MmuFilamentSync.synchronize(printer, callback)
            }
            assertTrue("Filament sync callback timed out", completed.await(20, TimeUnit.SECONDS))
            worker.join(2_000)
            failure.get()?.let { throw AssertionError("Test printer failed", it) }
            return requireNotNull(result.get())
        }
    }
}
