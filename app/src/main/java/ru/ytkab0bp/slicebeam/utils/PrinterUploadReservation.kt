package ru.ytkab0bp.slicebeam.utils

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Allows at most one in-flight upload/start flow for each configured printer origin. */
class PrinterUploadReservation {
    private val activeOrigins = ConcurrentHashMap.newKeySet<String>()

    fun reserve(configuredHost: String): String? {
        val origin = (PrinterUploadUrl.baseUrl(configuredHost) ?: configuredHost.trim().trimEnd('/'))
            .lowercase(Locale.ROOT)
        return origin.takeIf(activeOrigins::add)
    }

    fun release(origin: String) {
        activeOrigins.remove(origin)
    }
}
