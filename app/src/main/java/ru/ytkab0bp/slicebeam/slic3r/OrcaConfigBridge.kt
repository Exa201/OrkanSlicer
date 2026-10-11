package ru.ytkab0bp.slicebeam.slic3r

/** Result of asking the native Orca core for its canonical option schema. */
class OrcaConfigLoadResult internal constructor(
    val definition: PrintConfigDef?,
    val errorMessage: String?,
    val cause: Throwable?,
) {
    fun isSuccess(): Boolean = definition != null
}

/** Narrow Kotlin boundary for loading profile metadata from the existing JNI implementation. */
object OrcaConfigBridge {
    @JvmStatic
    fun loadPrintConfigDefinition(): OrcaConfigLoadResult {
        return try {
            val definition = PrintConfigDef()
            Native.get_print_config_def(definition)
            if (definition.options.isEmpty()) {
                val error = IllegalStateException("Der native Orca-Kern hat keine Profileinstellungen geliefert")
                OrcaConfigLoadResult(null, errorMessage(error), error)
            } else {
                OrcaConfigLoadResult(definition, null, null)
            }
        } catch (error: LinkageError) {
            OrcaConfigLoadResult(null, errorMessage(error), error)
        } catch (error: RuntimeException) {
            OrcaConfigLoadResult(null, errorMessage(error), error)
        }
    }

    @JvmStatic
    fun errorMessage(error: Throwable): String {
        return when (error) {
            is LinkageError -> "Der native Orca-Kern konnte nicht geladen werden. Prüfe die Gerätearchitektur und die mitgelieferten Bibliotheken."
            else -> error.message?.takeIf { it.isNotBlank() }?.let { "Das Orca-Einstellungsschema konnte nicht geladen werden: $it" }
                ?: "Das Orca-Einstellungsschema konnte nicht geladen werden."
        }
    }
}
