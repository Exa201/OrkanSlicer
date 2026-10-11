package ru.ytkab0bp.slicebeam.boot

import java.util.Collections

/** One startup operation with optional task dependencies and execution policy. */
open class BootTask {
    @JvmField val dependencies: List<Class<*>>
    @JvmField val run: Runnable
    @JvmField var workerThread: Boolean = false
    @JvmField var priority: Int = 0
    @JvmField var nonCritical: Boolean = false

    // AppBoot uses this package-internal scheduling slot from Java.
    @JvmField var index: Int = 0

    constructor(run: Runnable) {
        dependencies = Collections.emptyList()
        this.run = run
    }

    constructor(dependencies: List<Class<*>>, run: Runnable) {
        this.dependencies = dependencies
        this.run = run
    }

    open fun onWorker(): BootTask = onWorker(-20)

    open fun onWorker(priority: Int): BootTask {
        workerThread = true
        this.priority = priority
        return this
    }
}
