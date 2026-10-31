package ru.ytkab0bp.slicebeam.utils

import ru.ytkab0bp.slicebeam.config.ConfigObject

/** Stores only explicit, user-confirmed mappings in this app identity's private preferences. */
object QidiBoxToolMappingStore {
    fun load(printer: ConfigObject, toolCount: Int): IntArray {
        val key = QidiBoxToolMappingPolicy.preferenceKey(
            printer.title.orEmpty(), printer.get("print_host").orEmpty(),
        )
        return QidiBoxToolMappingPolicy.decode(Prefs.getPrefs()?.getString(key, null), toolCount)
    }

    fun save(printer: ConfigObject, mapping: IntArray) {
        val key = QidiBoxToolMappingPolicy.preferenceKey(
            printer.title.orEmpty(), printer.get("print_host").orEmpty(),
        )
        Prefs.getPrefs()?.edit()?.putString(key, QidiBoxToolMappingPolicy.encode(mapping))?.apply()
    }
}
