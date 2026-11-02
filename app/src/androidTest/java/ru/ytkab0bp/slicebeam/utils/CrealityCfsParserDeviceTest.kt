package ru.ytkab0bp.slicebeam.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrealityCfsParserDeviceTest {
    @Test
    fun mapsConnectedTraysKeepsEmptyGateIndicesAndNormalizesCrealityColors() {
        val response = JSONObject(
            """{"result":{"status":{"box":{
                "T1":{"state":"connect","material_type":["","000003","-1","000001"],"color_value":["","0FF1E1E","-1","000FF00"]},
                "T2":{"state":"None","material_type":["000002","-1","-1","-1"],"color_value":["0FFFFFF","-1","-1","-1"]}
            }}}}""",
        )
        val mmu = requireNotNull(CrealityCfsParser.toMmuResponse(response))
            .getJSONObject("result").getJSONObject("status").getJSONObject("mmu")

        assertEquals(16, mmu.getJSONArray("ttg_map").length())
        assertEquals(0, mmu.getJSONArray("gate_status").getInt(0))
        assertEquals(1, mmu.getJSONArray("gate_status").getInt(1))
        assertEquals("PETG", mmu.getJSONArray("gate_material").getString(1))
        assertEquals("FF1E1E", mmu.getJSONArray("gate_color").getString(1))
        assertEquals("PLA", mmu.getJSONArray("gate_material").getString(3))
        assertEquals("00FF00", mmu.getJSONArray("gate_color").getString(3))
        assertEquals(0, mmu.getJSONArray("gate_status").getInt(4))
    }

    @Test
    fun resolvesBrandedMaterialAndKeepsUnidentifiedColoredSpoolDistinct() {
        val response = JSONObject(
            """{"result":{"status":{"box":{
                "same_material":[["005001","0ffffff",["T1A"],"PLA"]],
                "T1":{"state":"connect","material_type":["005001","999999","-1","-1"],"color_value":["0ffffff","0A12B3C","-1","-1"]}
            }}}}""",
        )
        val mmu = requireNotNull(CrealityCfsParser.toMmuResponse(response))
            .getJSONObject("result").getJSONObject("status").getJSONObject("mmu")

        assertEquals("PLA", mmu.getJSONArray("gate_material").getString(0))
        assertEquals("FFFFFF", mmu.getJSONArray("gate_color").getString(0))
        assertEquals("unknown", mmu.getJSONArray("gate_material").getString(1))
        assertEquals("A12B3C", mmu.getJSONArray("gate_color").getString(1))
        assertEquals(1, mmu.getJSONArray("gate_status").getInt(1))
        assertTrue(MmuFilamentSync.fromMoonraker(JSONObject().put("result", JSONObject().put("status", JSONObject().put("mmu", mmu))))[1]?.unknownLoadedSpool == true)
        assertNull(MmuFilamentSync.fromMoonraker(JSONObject().put("result", JSONObject().put("status", JSONObject().put("mmu", mmu))))[2])
    }

    @Test
    fun rejectsMissingDisconnectedAndEmptyCfsResponses() {
        assertNull(CrealityCfsParser.toMmuResponse(JSONObject("{}")))
        val disconnected = JSONObject("""{"result":{"status":{"box":{"T1":{"state":"None"}}}}}""")
        assertNull(CrealityCfsParser.toMmuResponse(disconnected))
        val empty = JSONObject("""{"result":{"status":{"box":{"T1":{"state":"connect"}}}}}""")
        assertNull(CrealityCfsParser.toMmuResponse(empty))
    }
}
