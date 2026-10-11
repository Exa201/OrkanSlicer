package ru.ytkab0bp.slicebeam.slic3r

fun interface SliceListener {
    fun onProgress(progress: Int, text: String?)
}
