package ru.ytkab0bp.slicebeam.slic3r

/**
 * Replaces the result currently consumed by the GL viewer without releasing its native data
 * before the viewer has detached from it. Call on the GL thread.
 */
internal inline fun replaceGCodeResult(
    previous: GCodeProcessorResult?,
    next: GCodeProcessorResult?,
    installViewerResult: () -> Unit,
) {
    installViewerResult()
    if (previous !== next) previous?.release()
}
