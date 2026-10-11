package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

class ConfigSerializationNullValueTest {
    @Test
    fun nullProfileValuesAreSkippedInsteadOfAbortingTheSave() {
        val profile = ConfigObject("Printer A").apply {
            put("z_offset", "0.1")
            putJavaNull("optional_setting")
        }

        val serializedProfile = profile.serialize()
        val wrapper = Slic3rConfigWrapper().apply { printerConfigs.add(profile) }
        val serializedBundle = wrapper.serialize()

        assertTrue(serializedProfile.contains("z_offset = 0.1"))
        assertFalse(serializedProfile.contains("optional_setting"))
        assertTrue(serializedBundle.contains("z_offset = 0.1"))
        assertFalse(serializedBundle.contains("optional_setting"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun ConfigObject.putJavaNull(key: String) {
        (values as MutableMap<String, String?>)[key] = null
    }
}
