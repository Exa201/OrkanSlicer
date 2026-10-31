package ru.ytkab0bp.slicebeam.utils

import java.security.MessageDigest
import java.util.Locale

/** Private preference keys for MMU state, isolated by the selected printer profile title. */
internal object MmuPrinterPreferenceKeys {
    const val PREFIX = "mmu_printer_"

    fun forPrinter(state: String, printerTitle: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(printerTitle.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
        return "${PREFIX}${state}_$digest"
    }
}
