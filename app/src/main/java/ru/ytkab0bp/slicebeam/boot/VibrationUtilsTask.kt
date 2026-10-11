package ru.ytkab0bp.slicebeam.boot

import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.VibrationUtils

class VibrationUtilsTask : BootTask(Runnable { VibrationUtils.init(SliceBeam.requireInstance()) }) {
    init {
        onWorker()
    }
}
