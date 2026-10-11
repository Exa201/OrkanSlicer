package ru.ytkab0bp.slicebeam.boot

import java.util.Arrays
import ru.ytkab0bp.slicebeam.cloud.CloudController

open class CloudInitTask : BootTask(
    Arrays.asList<Class<*>>(
        PrefsTask::class.java,
        TrueTimeTask::class.java,
        LoadSlic3rConfigTask::class.java,
        CloudCachedInitTask::class.java
    ),
    Runnable { CloudController.init() }
) {
    init {
        onWorker()
        nonCritical = true
    }
}
