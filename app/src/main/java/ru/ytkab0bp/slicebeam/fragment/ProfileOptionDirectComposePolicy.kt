package ru.ytkab0bp.slicebeam.fragment

import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.config.ProfileStringVectorCodec

/** Identifies profile rows that can be rendered directly in the shared Compose list. */
object ProfileOptionDirectComposePolicy {
    enum class Kind {
        BOOLEAN, OPEN_ENUM, COLOR, NUMBER, ENUM, ENUM_VECTOR, VECTOR, POINT,
        STRING_VECTOR, STRING, FALLBACK,
    }

    @JvmStatic
    fun kind(
        definition: ConfigOptionDef,
        elementIndex: Int,
        rawValue: String,
    ): Kind {
        val integerOpenEnum = definition.guiType == ConfigOptionDef.GUIType.I_ENUM_OPEN &&
            definition.type == ConfigOptionDef.ConfigOptionType.INT
        val floatOpenEnum = definition.guiType == ConfigOptionDef.GUIType.F_ENUM_OPEN &&
            (definition.type == ConfigOptionDef.ConfigOptionType.FLOAT ||
                definition.type == ConfigOptionDef.ConfigOptionType.PERCENT ||
                definition.type == ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT)
        val enumChoices = definition.key == "host_type" ||
            minOf(definition.enumLabels?.size ?: 0, definition.enumValues?.size ?: 0) > 0

        return when {
            definition.type == ConfigOptionDef.ConfigOptionType.BOOL ||
                definition.type == ConfigOptionDef.ConfigOptionType.BOOLS -> Kind.BOOLEAN

            integerOpenEnum || floatOpenEnum -> Kind.OPEN_ENUM

            definition.guiType == ConfigOptionDef.GUIType.COLOR -> Kind.COLOR

            definition.type == ConfigOptionDef.ConfigOptionType.ENUM && enumChoices -> Kind.ENUM

            definition.type == ConfigOptionDef.ConfigOptionType.ENUMS && enumChoices -> Kind.ENUM_VECTOR

            definition.type == ConfigOptionDef.ConfigOptionType.INTS ||
                definition.type == ConfigOptionDef.ConfigOptionType.PERCENTS ||
                definition.type == ConfigOptionDef.ConfigOptionType.FLOATS_OR_PERCENTS ||
                definition.type == ConfigOptionDef.ConfigOptionType.FLOATS -> Kind.VECTOR

            definition.type == ConfigOptionDef.ConfigOptionType.POINT ||
                definition.type == ConfigOptionDef.ConfigOptionType.POINTS ||
                definition.type == ConfigOptionDef.ConfigOptionType.POINT3 -> Kind.POINT

            definition.type == ConfigOptionDef.ConfigOptionType.INT ||
                definition.type == ConfigOptionDef.ConfigOptionType.FLOAT ||
                definition.type == ConfigOptionDef.ConfigOptionType.PERCENT ||
                definition.type == ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT -> Kind.NUMBER

            definition.type == ConfigOptionDef.ConfigOptionType.STRINGS &&
                definition.guiType != ConfigOptionDef.GUIType.ONE_STRING &&
                elementIndex == -1 && ProfileStringVectorCodec.parse(rawValue) != null -> Kind.STRING_VECTOR

            (definition.type == ConfigOptionDef.ConfigOptionType.STRING ||
                (definition.type == ConfigOptionDef.ConfigOptionType.STRINGS &&
                    definition.guiType == ConfigOptionDef.GUIType.ONE_STRING)) &&
                elementIndex == -1 -> Kind.STRING

            else -> Kind.FALLBACK
        }
    }
}
