package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class ConfigMetadataInteropTest {
    @Test
    fun configOptionFieldsRetainNativeNamesAndDescriptors() {
        val expectedFields = mapOf(
            "key" to String::class.java,
            "type" to ConfigOptionDef.ConfigOptionType::class.java,
            "guiType" to ConfigOptionDef.GUIType::class.java,
            "label" to String::class.java,
            "fullLabel" to String::class.java,
            "printerTechnology" to ConfigOptionDef.PrinterTechnology::class.java,
            "category" to String::class.java,
            "tooltip" to String::class.java,
            "sidetext" to String::class.java,
            "multiline" to Boolean::class.javaPrimitiveType,
            "fullWidth" to Boolean::class.javaPrimitiveType,
            "readonly" to Boolean::class.javaPrimitiveType,
            "height" to Int::class.javaPrimitiveType,
            "width" to Int::class.javaPrimitiveType,
            "min" to Float::class.javaPrimitiveType,
            "max" to Float::class.javaPrimitiveType,
            "mode" to ConfigOptionDef.ConfigOptionMode::class.java,
            "defaultValue" to String::class.java,
            "enumLabels" to Array<String>::class.java,
            "enumValues" to Array<String>::class.java,
        )

        expectedFields.forEach { (name, type) ->
            val field = ConfigOptionDef::class.java.getField(name)
            assertEquals("JNI descriptor for $name", type, field.type)
            assertTrue("JNI field $name must remain public", Modifier.isPublic(field.modifiers))
            assertFalse("JNI field $name must remain an instance field", Modifier.isStatic(field.modifiers))
        }
        assertNotNull(ConfigOptionDef::class.java.getDeclaredConstructor())
    }

    @Test
    fun optionDefaultsLabelsAndEnumListFlagsRemainCompatible() {
        val option = ConfigOptionDef()
        assertEquals(ConfigOptionDef.ConfigOptionType.NONE, option.type)
        assertEquals(ConfigOptionDef.PrinterTechnology.UNKNOWN, option.printerTechnology)
        assertEquals(ConfigOptionDef.ConfigOptionMode.SIMPLE, option.mode)
        assertEquals(Float.MIN_VALUE, option.min)
        assertEquals(Float.MAX_VALUE, option.max)
        assertEquals(-1, option.height)
        assertEquals(-1, option.width)
        assertEquals("", option.getLabel())
        assertEquals("", option.getFullLabel())

        option.label = "Kurz"
        option.fullLabel = "Vollständige Bezeichnung"
        assertEquals("Kurz", option.getLabel())
        assertEquals("Vollständige Bezeichnung", option.getFullLabel())
        option.label = ""
        assertEquals("Vollständige Bezeichnung", option.getLabel())

        assertFalse(ConfigOptionDef.ConfigOptionType.FLOAT.list)
        assertTrue(ConfigOptionDef.ConfigOptionType.FLOATS.list)
        assertTrue(ConfigOptionDef.ConfigOptionType.POINTS.list)
        assertFalse(ConfigOptionDef.ConfigOptionType.ENUMS.list)
    }

    @Test
    fun printConfigDefKeepsJniEntryPointsAndNativeEnumLookup() {
        val definition = PrintConfigDef()
        val option = ConfigOptionDef().apply { key = "layer_height" }
        definition.addOption("layer_height", option)
        assertSame(option, definition.options["layer_height"])

        val addOption = PrintConfigDef::class.java.getDeclaredMethod(
            "addOption",
            String::class.java,
            ConfigOptionDef::class.java,
        )
        assertEquals(Void.TYPE, addOption.returnType)

        val resolveEnum = PrintConfigDef::class.java.getDeclaredMethod(
            "resolveEnum",
            String::class.java,
            String::class.java,
        )
        assertEquals(Any::class.java, resolveEnum.returnType)
        assertTrue(Modifier.isStatic(resolveEnum.modifiers))
        assertTrue(Modifier.isSynchronized(resolveEnum.modifiers))
        assertEquals(
            ConfigOptionDef.ConfigOptionType.FLOATS,
            PrintConfigDef.resolveEnum(
                "ru/ytkab0bp/slicebeam/slic3r/ConfigOptionDef\$ConfigOptionType",
                "FLOATS",
            ),
        )

        val skipDefaults = PrintConfigDef::class.java.getField("SKIP_DEFAULT_OPTIONS")
        assertTrue(Modifier.isStatic(skipDefaults.modifiers))
        assertTrue(PrintConfigDef.SKIP_DEFAULT_OPTIONS.contains("tower_speed"))

        val singleton = PrintConfigDef::class.java.getDeclaredField("instance")
        assertTrue(Modifier.isStatic(singleton.modifiers))
        assertTrue(Modifier.isPrivate(singleton.modifiers))
        val getInstance = PrintConfigDef::class.java.getDeclaredMethod("getInstance")
        assertTrue(Modifier.isStatic(getInstance.modifiers))
        assertTrue(Modifier.isSynchronized(getInstance.modifiers))
    }
}
