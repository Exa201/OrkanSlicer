package ru.ytkab0bp.slicebeam.config

/** Identifies a private process copy for one printer without changing bundled presets. */
object PrinterScopedProcessProfile {
    fun title(source: String, printer: String): String = "$source · $printer"

    fun belongsTo(profile: ConfigObject, printer: String): Boolean =
        profile.title?.endsWith(" · $printer") == true &&
            ProfileStringVectorCodec.parse(profile["compatible_printers"].orEmpty()) == listOf(printer)

    fun copy(source: ConfigObject, printer: String): ConfigObject = ConfigObject(source).apply {
        setTitle(PrinterScopedProcessProfile.title(source.title.orEmpty(), printer))
        put("compatible_printers", ProfileStringVectorCodec.serialize(listOf(printer)))
        put("compatible_printers_condition", "")
        profileListType = ConfigObject.PROFILE_LIST_PRINT
    }
}
