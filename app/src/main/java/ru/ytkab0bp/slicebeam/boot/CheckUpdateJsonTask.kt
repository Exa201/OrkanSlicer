package ru.ytkab0bp.slicebeam.boot

import java.io.IOException
import ru.ytkab0bp.slicebeam.SliceBeam

class CheckUpdateJsonTask : BootTask(Runnable {
    try {
        SliceBeam.requireInstance().getAssets().open("update.json").close()
        SliceBeam.hasUpdateInfo = true
    } catch (_: IOException) {
        SliceBeam.hasUpdateInfo = false
    }
}) {
    init {
        onWorker()
    }
}
