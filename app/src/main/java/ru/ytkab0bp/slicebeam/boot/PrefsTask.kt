package ru.ytkab0bp.slicebeam.boot

import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.Prefs

class PrefsTask : BootTask(Runnable { Prefs.init(SliceBeam.requireInstance()) })
