package ru.ytkab0bp.slicebeam.slic3r

import java.util.Locale

/** Formats whose independent meshes can safely be merged into one import transaction. */
internal object ModelBatchImportPolicy {
    private val supportedExtensions = setOf("stl", "obj", "amf", "oltp")

    fun supports(fileName: String?): Boolean {
        if (fileName.isNullOrBlank()) return false
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension in supportedExtensions
    }
}
