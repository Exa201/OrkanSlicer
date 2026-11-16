package ru.ytkab0bp.slicebeam.boot

import org.junit.Assert.assertTrue
import org.junit.Test

class PrintConfigWarmupTaskTest {
    @Test
    fun nativeOptionCatalogWarmupRunsOffMainThreadWithoutBlockingAppStartup() {
        val task = PrintConfigWarmupTask()

        assertTrue("The native option catalog must be warmed on a worker", task.workerThread)
        assertTrue("A lazy preview/catalog optimization must not delay the first screen", task.nonCritical)
    }
}
