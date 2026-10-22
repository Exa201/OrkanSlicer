package ru.ytkab0bp.slicebeam.slic3r

/** Resolves the import name exposed by Android document providers, including providers that
 * publish STEP files with a generic name and rely on the document MIME type. */
internal object ModelImportFileName {
    private val stepMimeTypes = setOf(
        "model/step",
        "application/step",
        "application/x-step",
        "application/vnd.step",
        "application/step-file",
    )

    fun resolve(displayName: String?, uriPath: String?, mimeType: String?): String? {
        val name = displayName?.trim()?.takeIf(String::isNotEmpty)
            ?: uriPath?.substringAfterLast('/')?.trim()?.takeIf(String::isNotEmpty)
        val normalizedMimeType = mimeType?.substringBefore(';')?.trim()?.lowercase(java.util.Locale.ROOT)
        if (normalizedMimeType != null && normalizedMimeType in stepMimeTypes) {
            return when {
                name == null -> "model.step"
                StepImportOptions.isStepFile(name) -> name
                name.substringAfterLast('/', name).contains('.') -> name
                else -> "$name.step"
            }
        }
        return name
    }
}
