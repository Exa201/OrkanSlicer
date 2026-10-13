package ru.ytkab0bp.slicebeam.slic3r

/** Called on the native model-loading thread; 0–0.5 reads facets, then geometry is prepared. */
fun interface ImportProgressCallback {
    fun onProgress(fraction: Float)
}
