package ru.ytkab0bp.slicebeam.slic3r

import java.io.File

/** Java-friendly result for one native model import. A zero pointer always means failure. */
class OrcaModelLoadResult internal constructor(
    val pointer: Long,
    val errorMessage: String?,
    val cause: Throwable?,
) {
    fun isSuccess(): Boolean = pointer != 0L
}

/** Narrow Kotlin boundary for loading one STL/3MF plate through the existing Orca JNI entry point. */
object OrcaModelBridge {
    @JvmStatic
    fun load(path: String, plateId: Int, progress: ImportProgressCallback? = null): OrcaModelLoadResult {
        val source = File(path)
        val validationError = validateRequest(path, plateId, source.isFile, source.canRead())
        if (validationError != null) {
            return failed(Slic3rRuntimeError(validationError), validationError)
        }

        return try {
            val pointer = Native.model_read_from_file(path, baseName(source.name), plateId, progress)
            if (pointer == 0L) {
                val error = Slic3rRuntimeError("Der Orca-Kern hat kein Modell zurückgegeben.")
                failed(error, errorMessage(error))
            } else {
                OrcaModelLoadResult(pointer, null, null)
            }
        } catch (error: Slic3rRuntimeError) {
            failed(error, errorMessage(error))
        } catch (error: LinkageError) {
            failed(error, errorMessage(error))
        } catch (error: RuntimeException) {
            failed(error, errorMessage(error))
        }
    }

    @JvmStatic
    fun validateRequest(path: String?, plateId: Int, isFile: Boolean, canRead: Boolean): String? {
        if (path.isNullOrBlank()) return "Es wurde kein Dateipfad zum Modell angegeben."
        if (plateId < 0) return "Die ausgewählte Druckplatte ist ungültig."
        if (!isFile) return "Die Modelldatei wurde nicht gefunden oder ist kein lesbares Dateiformat."
        if (!canRead) return "Die Modelldatei kann nicht gelesen werden. Prüfe den Speicherzugriff."
        return null
    }

    @JvmStatic
    fun baseName(fileName: String): String {
        val separator = maxOf(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))
        val name = fileName.substring(separator + 1)
        val extension = name.lastIndexOf('.')
        return if (extension > 0) name.substring(0, extension) else name
    }

    @JvmStatic
    fun errorMessage(error: Throwable): String = when (error) {
        is LinkageError -> "Der native Orca-Kern konnte das Modell nicht laden. Prüfe die Gerätearchitektur und die mitgelieferten Bibliotheken."
        else -> error.message?.takeIf { it.isNotBlank() }
            ?: "Das Modell konnte nicht geladen werden. Prüfe, ob die Datei beschädigt oder unvollständig ist."
    }

    private fun failed(error: Throwable, message: String) = OrcaModelLoadResult(0L, message, error)
}
