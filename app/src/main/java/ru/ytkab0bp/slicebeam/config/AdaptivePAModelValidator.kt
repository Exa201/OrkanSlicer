package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.config.ProfileStringVectorCodec

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

    /** Slice-time warning, indexed by the logical filament identity shown to the user. */
    data class SliceWarning(val filamentNumber: Int, val lineNumber: Int, val problem: Problem)

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

    /**
     * Reports malformed Adaptive PA measurements only for enabled filaments used by this slice.
     * `filament_self_index` maps variant columns to logical filaments in Orca's config vector.
     */
    @JvmStatic
    fun sliceWarnings(config: Map<String, String>, usedFilamentIds: Set<Int>): List<SliceWarning> {
        if (usedFilamentIds.isEmpty()) return emptyList()
        val models = ProfileStringVectorCodec.parse(config["adaptive_pressure_advance_model"].orEmpty())
            ?: return emptyList()
        val adaptive = ProfileStringVectorCodec.parse(config["adaptive_pressure_advance"].orEmpty())
            ?: return emptyList()
        val pressureAdvance = ProfileStringVectorCodec.parse(config["enable_pressure_advance"].orEmpty())
            ?: return emptyList()
        val selfIndices = ProfileStringVectorCodec.parse(config["filament_self_index"].orEmpty())
        val hasSelfIndexMap = selfIndices?.size == models.size

        return models.mapIndexedNotNull { variantIndex, model ->
            val filamentId = if (hasSelfIndexMap) {
                selfIndices?.get(variantIndex)?.toIntOrNull()?.minus(1) ?: return@mapIndexedNotNull null
            } else variantIndex
            if (filamentId !in usedFilamentIds || adaptive.getOrNull(variantIndex).isNotEnabled() ||
                pressureAdvance.getOrNull(variantIndex).isNotEnabled()) return@mapIndexedNotNull null
            val (lineNumber, problem) = firstLineIssue(model) ?: return@mapIndexedNotNull null
            SliceWarning(filamentNumber = filamentId + 1, lineNumber = lineNumber, problem = problem)
        }
    }

    private fun String?.isNotEnabled(): Boolean = this != "1" && !equals("true", ignoreCase = true)

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
