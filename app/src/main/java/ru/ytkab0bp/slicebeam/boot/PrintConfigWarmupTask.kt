package ru.ytkab0bp.slicebeam.boot

import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef

class PrintConfigWarmupTask : BootTask(Runnable { PrintConfigDef.getInstance() }) {
    init {
        onWorker()
    }
}
