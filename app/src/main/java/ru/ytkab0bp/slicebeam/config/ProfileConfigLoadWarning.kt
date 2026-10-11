package ru.ytkab0bp.slicebeam.config

/** Structured, non-fatal issues found while reading a profile bundle. */
sealed interface ProfileConfigLoadWarning {
    data class MissingParent(val profile: String, val parent: String) : ProfileConfigLoadWarning
    data class UnknownOption(val profile: String, val key: String) : ProfileConfigLoadWarning
    data class InvalidValue(
        val profile: String,
        val key: String,
        val value: String,
        val issue: ValueIssue,
    ) : ProfileConfigLoadWarning

    data object ValidationUnavailable : ProfileConfigLoadWarning

    enum class ValueIssue {
        INVALID_NUMBER,
        OUT_OF_RANGE,
        UNKNOWN_ENUM,
        INVALID_BOOLEAN,
        INVALID_POINT,
    }
}
