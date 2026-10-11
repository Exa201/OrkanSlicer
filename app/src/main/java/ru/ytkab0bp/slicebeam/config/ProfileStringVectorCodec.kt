package ru.ytkab0bp.slicebeam.config

/** Codec for Orca's semicolon-separated, C-style escaped ConfigOptionStrings representation. */
object ProfileStringVectorCodec {
    @JvmStatic
    fun parse(serialized: String): List<String>? {
        if (serialized.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var index = 0

        while (true) {
            while (index < serialized.length && (serialized[index] == ' ' || serialized[index] == '\t')) index++
            if (index == serialized.length) return result

            val value = StringBuilder()
            if (serialized[index] == '"') {
                index++
                var closed = false
                while (index < serialized.length) {
                    var char = serialized[index++]
                    if (char == '"') {
                        closed = true
                        break
                    }
                    if (char == '\\') {
                        if (index == serialized.length) return null
                        char = when (val escaped = serialized[index++]) {
                            'r' -> '\r'
                            'n' -> '\n'
                            else -> escaped
                        }
                    }
                    value.append(char)
                }
                if (!closed) return null
            } else {
                while (index < serialized.length && serialized[index] != ';') {
                    value.append(serialized[index++])
                }
            }
            result += value.toString()
            if (index == serialized.length) return result

            while (index < serialized.length && (serialized[index] == ' ' || serialized[index] == '\t')) index++
            if (index == serialized.length) return result
            if (serialized[index] != ';') return null
            index++
            if (index == serialized.length) {
                result += ""
                return result
            }
        }
    }

    @JvmStatic
    fun serialize(values: List<String>): String = values.map { value ->
        val quote = (values.size == 1 && value.isEmpty()) || value.any {
            it == ' ' || it == '\t' || it == '\\' || it == '"' || it == '\r' || it == '\n' || it == ';'
        }
        val escaped = buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\', '"' -> append('\\').append(char)
                    '\r' -> append("\\r")
                    '\n' -> append("\\n")
                    else -> append(char)
                }
            }
        }
        if (quote) "\"$escaped\"" else escaped
    }.joinToString(";")

    @JvmStatic
    fun preview(serialized: String): String {
        val values = parse(serialized) ?: return "—"
        if (values.isEmpty()) return "—"
        val sample = values.take(2).joinToString(" · ") { value ->
            value.replace('\n', ' ').replace('\r', ' ').ifEmpty { "(leer)" }
        }
        return if (values.size > 2) "$sample …" else sample
    }
}
