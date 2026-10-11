package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

/** Applies the exact profile-name character restriction used by Orca's config wrapper. */
object ProfileNameValidator {
    private val forbiddenCharacters = Slic3rConfigWrapper.BLACKLISTED_SYMBOLS.toSet()

    @JvmStatic
    fun isValid(value: String): Boolean = value.isNotBlank() && value.none(forbiddenCharacters::contains)

    @JvmStatic
    fun conflictsWithExisting(value: String, existingNames: Iterable<String?>, currentName: String?): Boolean {
        val candidate = value.trim()
        val current = currentName?.trim()
        return existingNames.filterNotNull().any { existing ->
            val normalized = existing.trim()
            normalized.equals(candidate, ignoreCase = true) &&
                !normalized.equals(current, ignoreCase = true)
        }
    }
}
