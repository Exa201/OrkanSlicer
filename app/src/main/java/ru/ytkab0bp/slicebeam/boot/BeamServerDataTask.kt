package ru.ytkab0bp.slicebeam.boot

import org.json.JSONException
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.BeamServerData
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.Prefs

open class BeamServerDataTask : BootTask(Runnable {
    try {
        SliceBeam.SERVER_DATA = BeamServerData(JSONObject(Prefs.getBeamServerData()))
    } catch (_: JSONException) {
        SliceBeam.SERVER_DATA = BeamServerData(JSONObject())
    }
}) {
    init {
        onWorker()
    }
}
