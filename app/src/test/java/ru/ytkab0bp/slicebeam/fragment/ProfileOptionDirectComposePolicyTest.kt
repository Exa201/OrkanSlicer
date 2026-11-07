package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

class ProfileOptionDirectComposePolicyTest {
    @Test
    fun printerApiKeyIsAlwaysTreatedAsPasswordInEveryProfileRenderPath() {
        val apiKey = ConfigOptionDef().apply {
            key = "printhost_apikey"
            type = ConfigOptionDef.ConfigOptionType.STRING
        }
        val ordinaryHost = ConfigOptionDef().apply {
            key = "print_host"
            type = ConfigOptionDef.ConfigOptionType.STRING
        }
        val nativePassword = ConfigOptionDef().apply {
            key = "printhost_password"
            type = ConfigOptionDef.ConfigOptionType.STRING
            guiType = ConfigOptionDef.GUIType.PASSWORD
        }

        assertEquals(true, ProfileOptionComposePresenter.isPasswordOption(apiKey))
        assertEquals(false, ProfileOptionComposePresenter.isPasswordOption(ordinaryHost))
        assertEquals(true, ProfileOptionComposePresenter.isPasswordOption(nativePassword))
    }

    @Test
    fun scalarRowsPreserveBooleanNumericAndOpenEnumRendering() {
        assertKind(ConfigOptionDef.ConfigOptionType.BOOL, expected = ProfileOptionDirectComposePolicy.Kind.BOOLEAN)
        assertKind(ConfigOptionDef.ConfigOptionType.BOOLS, expected = ProfileOptionDirectComposePolicy.Kind.BOOLEAN)
        assertKind(ConfigOptionDef.ConfigOptionType.PERCENT, expected = ProfileOptionDirectComposePolicy.Kind.NUMBER)
        assertKind(ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT, expected = ProfileOptionDirectComposePolicy.Kind.NUMBER)
        assertKind(
            ConfigOptionDef.ConfigOptionType.INT,
            guiType = ConfigOptionDef.GUIType.I_ENUM_OPEN,
            expected = ProfileOptionDirectComposePolicy.Kind.OPEN_ENUM,
        )
        assertKind(
            ConfigOptionDef.ConfigOptionType.FLOAT,
            guiType = ConfigOptionDef.GUIType.F_ENUM_OPEN,
            expected = ProfileOptionDirectComposePolicy.Kind.OPEN_ENUM,
        )
    }

    @Test
    fun enumVectorPointAndNumericVectorTypesKeepSpecializedEditors() {
        assertKind(
            ConfigOptionDef.ConfigOptionType.ENUM,
            labels = arrayOf("Auto", "Manual"),
            values = arrayOf("auto", "manual"),
            expected = ProfileOptionDirectComposePolicy.Kind.ENUM,
        )
        assertKind(
            ConfigOptionDef.ConfigOptionType.ENUMS,
            labels = arrayOf("Red", "Blue"),
            values = arrayOf("red", "blue"),
            expected = ProfileOptionDirectComposePolicy.Kind.ENUM_VECTOR,
        )
        assertKind(ConfigOptionDef.ConfigOptionType.INTS, expected = ProfileOptionDirectComposePolicy.Kind.VECTOR)
        assertKind(ConfigOptionDef.ConfigOptionType.PERCENTS, expected = ProfileOptionDirectComposePolicy.Kind.VECTOR)
        assertKind(ConfigOptionDef.ConfigOptionType.POINT, expected = ProfileOptionDirectComposePolicy.Kind.POINT)
        assertKind(ConfigOptionDef.ConfigOptionType.POINTS, expected = ProfileOptionDirectComposePolicy.Kind.POINT)
        assertKind(ConfigOptionDef.ConfigOptionType.POINT3, expected = ProfileOptionDirectComposePolicy.Kind.POINT)
        assertKind(ConfigOptionDef.ConfigOptionType.ENUM, expected = ProfileOptionDirectComposePolicy.Kind.FALLBACK)
        assertKind(
            ConfigOptionDef.ConfigOptionType.FLOAT,
            guiType = ConfigOptionDef.GUIType.F_ENUM_OPEN,
            expected = ProfileOptionDirectComposePolicy.Kind.OPEN_ENUM,
        )
    }

    @Test
    fun colorStringsAndUnknownSchemaValuesAlwaysUseAComposeRenderer() {
        assertKind(
            ConfigOptionDef.ConfigOptionType.STRING,
            guiType = ConfigOptionDef.GUIType.COLOR,
            expected = ProfileOptionDirectComposePolicy.Kind.COLOR,
        )
        assertKind(ConfigOptionDef.ConfigOptionType.STRING, expected = ProfileOptionDirectComposePolicy.Kind.STRING)
        assertKind(
            ConfigOptionDef.ConfigOptionType.STRINGS,
            value = "first;second",
            expected = ProfileOptionDirectComposePolicy.Kind.STRING_VECTOR,
        )
        assertKind(
            ConfigOptionDef.ConfigOptionType.STRINGS,
            value = "first;second",
            elementIndex = 0,
            expected = ProfileOptionDirectComposePolicy.Kind.FALLBACK,
        )
        assertKind(ConfigOptionDef.ConfigOptionType.NONE, expected = ProfileOptionDirectComposePolicy.Kind.FALLBACK)
    }

    private fun assertKind(
        type: ConfigOptionDef.ConfigOptionType,
        guiType: ConfigOptionDef.GUIType? = null,
        labels: Array<String>? = null,
        values: Array<String>? = null,
        value: String = "",
        elementIndex: Int = -1,
        expected: ProfileOptionDirectComposePolicy.Kind,
    ) {
        val definition = ConfigOptionDef().apply {
            this.type = type
            this.guiType = guiType
            this.enumLabels = labels
            this.enumValues = values
        }
        assertEquals(expected, ProfileOptionDirectComposePolicy.kind(definition, elementIndex, value))
    }
}
