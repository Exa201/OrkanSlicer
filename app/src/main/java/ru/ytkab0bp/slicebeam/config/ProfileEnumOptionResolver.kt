package ru.ytkab0bp.slicebeam.config

/** Resolves current Orca enum values, including older profiles that saved the option index. */
object ProfileEnumOptionResolver {
    @JvmStatic
    fun parseValues(serialized: String): LinkedHashSet<String> =
        serialized.split(',').map(String::trim).filter(String::isNotEmpty).toCollection(LinkedHashSet())

    /** Keeps the prior order, then adds new selections in the Orca schema's order. */
    @JvmStatic
    fun serializeSelection(values: Array<String>, selectedValues: Iterable<String>, previousValue: String): String {
        val remaining = selectedValues.toCollection(LinkedHashSet())
        val ordered = LinkedHashSet<String>()
        for (previous in parseValues(previousValue)) {
            if (remaining.remove(previous)) ordered.add(previous)
        }
        for (value in values) {
            if (remaining.remove(value)) ordered.add(value)
        }
        ordered.addAll(remaining)
        return ordered.joinToString(",")
    }

    @JvmStatic
    fun selectedIndex(value: String, values: Array<String>, optionCount: Int = values.size): Int {
        val count = optionCount.coerceIn(0, values.size)
        for (index in 0 until count) {
            if (values[index] == value) return index
        }
        return value.toIntOrNull()?.takeIf { it in 0 until count } ?: -1
    }

    @JvmStatic
    fun displayLabel(value: String, labels: Array<String>, values: Array<String>): String {
        val index = selectedIndex(value, values, minOf(labels.size, values.size))
        return labels.getOrNull(index) ?: value
    }
}
