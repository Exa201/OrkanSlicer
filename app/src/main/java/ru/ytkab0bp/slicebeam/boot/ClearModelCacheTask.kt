package ru.ytkab0bp.slicebeam.boot

import ru.ytkab0bp.slicebeam.SliceBeam

class ClearModelCacheTask : BootTask(Runnable {
    val cache = SliceBeam.getModelCacheDir()
    if (cache.exists()) {
        cache.listFiles()?.forEach { it.delete() }
    }
}) {
    init {
        nonCritical = true
        onWorker()
    }
}
