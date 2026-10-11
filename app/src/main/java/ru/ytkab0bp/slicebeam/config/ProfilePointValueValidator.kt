package ru.ytkab0bp.slicebeam.config

/** Validates Orca point options and converts locale-friendly editor text to Orca's serialized form. */
object ProfilePointValueValidator {
    enum class Kind { POINT, POINTS, POINT3 }
    enum class Result { VALID, EMPTY, INVALID_COORDINATE }

    private data class ParsedCoordinate(val value: String)

    fun formatForEditor(value: String, kind: Kind): String {
        if (value.isBlank()) return ""
        if (value.contains(';') || value.contains('\n') || value.contains('\r')) return value
        return when (kind) {
            Kind.POINT -> value.split(Regex("[,xX]")).joinToString("; ")
            Kind.POINT3 -> value.split(Regex("[,xX]")).joinToString("; ")
            Kind.POINTS -> value.split(',').joinToString("\n") { it.trim().replace('x', ';') }
        }
    }

    private fun parsePoint(raw: String, kind: Kind): List<ParsedCoordinate>? {
        val expected = if (kind == Kind.POINT3) 3 else 2
        val delimiter = when {
            ';' in raw -> ';'
            'x' in raw || 'X' in raw -> if ('x' in raw) 'x' else 'X'
            ',' in raw -> ','
            else -> return null
        }
        val parts = raw.split(delimiter).map(String::trim)
        if (parts.size != expected || parts.any(String::isBlank)) return null
        val decimalCommaAllowed = delimiter != ','
        val parsed = parts.map { part ->
            val normalized = if (decimalCommaAllowed && part.count { it == ',' } == 1 && '.' !in part) {
                part.replace(',', '.')
            } else {
                part
            }
            val number = normalized.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
            ParsedCoordinate(normalized)
        }
        return parsed
    }

    private fun parsePoints(value: String): List<List<ParsedCoordinate>>? {
        if (value.isBlank()) return emptyList()
        val lines = when {
            '\n' in value || '\r' in value || '|' in value -> value.split(Regex("[\\r\\n|]+"))
            ';' in value -> listOf(value) // A single locale-friendly point may use decimal commas.
            else -> value.split(',') // Orca's stored vector form separates x-delimited points with commas.
        }.map(String::trim).filter(String::isNotEmpty)
        if (lines.isEmpty()) return emptyList()
        return lines.map { parsePoint(it, Kind.POINT) ?: return null }
    }

    fun validate(value: String, kind: Kind): Result {
        if (value.isBlank()) return if (kind == Kind.POINTS) Result.VALID else Result.EMPTY
        val points = when (kind) {
            Kind.POINT -> listOf(parsePoint(value, Kind.POINT) ?: return Result.INVALID_COORDINATE)
            Kind.POINT3 -> listOf(parsePoint(value, Kind.POINT3) ?: return Result.INVALID_COORDINATE)
            Kind.POINTS -> parsePoints(value) ?: return Result.INVALID_COORDINATE
        }
        return if (points.all { point -> point.all { it.value.toDoubleOrNull()?.isFinite() == true } }) {
            Result.VALID
        } else {
            Result.INVALID_COORDINATE
        }
    }

    fun normalizeForStorage(value: String, kind: Kind): String {
        if (value.isBlank()) return ""
        return when (kind) {
            Kind.POINT, Kind.POINT3 -> {
                val point = requireNotNull(parsePoint(value, kind))
                point.joinToString(",") { it.value }
            }
            Kind.POINTS -> requireNotNull(parsePoints(value)).joinToString(",") { point ->
                point.joinToString("x") { it.value }
            }
        }
    }

    fun preview(value: String, kind: Kind): String {
        if (value.isBlank()) return "—"
        val formatted = formatForEditor(value, kind)
        val visible = formatted.lineSequence().map(String::trim).filter(String::isNotEmpty).take(2)
            .joinToString(" · ")
        val total = formatted.lineSequence().count(String::isNotBlank)
        return if (total > 2) "$visible …" else visible
    }
}
