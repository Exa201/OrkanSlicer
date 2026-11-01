package ru.ytkab0bp.slicebeam.config

/** Validation used by Orca's adaptive pressure advance profile editor. */
object AdaptivePAModelValidator {
    enum class Problem {
        INVALID_CHARACTERS,
        VALUE_COUNT,
        INVALID_NUMBER,
        PA_TOO_HIGH,
        FLOW_NOT_GREATER_THAN_PA,
        ACCELERATION_NOT_GREATER_THAN_FLOW,
    }

    data class Issue(val variantIndex: Int, val lineNumber: Int, val problem: Problem)

    /** Only rows changed in the editor are validated; untouched variants may keep Orca defaults. */
    @JvmStatic
    fun firstEditedVariantIssue(before: List<String>, after: List<String>): Issue? {
        after.forEachIndexed { variantIndex, value ->
            if (before.getOrNull(variantIndex) == value) return@forEachIndexed
            firstLineIssue(value)?.let { (lineNumber, problem) ->
                return Issue(variantIndex, lineNumber, problem)
            }
        }
        return null
    }

    private fun firstLineIssue(model: String): Pair<Int, Problem>? {
        model.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            val values = line.split(',').map { it.trim() }
            if (values.size != 3) return (index + 1) to Problem.VALUE_COUNT
            if (values.any { value -> value.any { it !in '0'..'9' && it != '.' } })
                return (index + 1) to Problem.INVALID_CHARACTERS
            val parsed = values.map { it.toDoubleOrNull() }
            if (parsed.any { it == null || !it.isFinite() })
                return (index + 1) to Problem.INVALID_NUMBER

            val pa = parsed[0]!!
            val flow = parsed[1]!!
            val acceleration = parsed[2]!!
            if (pa >= 2.0) return (index + 1) to Problem.PA_TOO_HIGH
            if (flow <= pa) return (index + 1) to Problem.FLOW_NOT_GREATER_THAN_PA
            if (acceleration <= flow)
                return (index + 1) to Problem.ACCELERATION_NOT_GREATER_THAN_FLOW
        }
        return null
    }
}
