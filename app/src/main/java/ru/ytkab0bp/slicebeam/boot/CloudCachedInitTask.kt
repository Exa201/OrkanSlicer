package ru.ytkab0bp.slicebeam.boot

import java.util.Collections
import ru.ytkab0bp.slicebeam.cloud.CloudController

open class CloudCachedInitTask : BootTask(
    Collections.singletonList(PrefsTask::class.java),
    Runnable { CloudController.initCached() }
) {
    init {
        onWorker()
    }
}
