package ru.ytkab0bp.slicebeam.config

/** Keeps Orca's open-enum suggestions paired while still allowing custom values. */
object ProfileOpenOptionResolver {
    data class Choice(val label: String, val value: String)

    @JvmStatic
    fun choices(
        labels: Array<String>?,
        values: Array<String>?,
        integerEnum: Boolean,
    ): List<Choice> {
        val safeLabels = labels ?: emptyArray()
        val safeValues = values ?: emptyArray()
        if (safeValues.isEmpty()) {
            if (!integerEnum || safeLabels.isEmpty()) return emptyList()
            return safeLabels.mapIndexed { index, label ->
                Choice(label.ifBlank { index.toString() }, index.toString())
            }
        }
        return safeValues.mapIndexed { index, value ->
            val label = safeLabels.getOrNull(index)?.takeIf { it.isNotBlank() } ?: value
            Choice(label, value)
        }
    }

    @JvmStatic
    fun displayValue(value: String, choices: List<Choice>): String =
        choices.firstOrNull { it.value == value }?.label ?: value
}
