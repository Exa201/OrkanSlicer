package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.config.ConfigObject
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class Slic3rConfigWrapperEncodingTest {
    @Test
    fun printerScopedUnicodeProfileTitleSurvivesUtf8ConfigRoundTrip() {
        val title = "MyPrint · MyPrinter"
        val config = Slic3rConfigWrapper().apply {
            printConfigs.add(ConfigObject(title).apply {
                profileListType = ConfigObject.PROFILE_LIST_PRINT
                put("layer_height", "0.2")
            })
        }

        val serialized = config.serialize().toByteArray(StandardCharsets.UTF_8)
        val reloaded = Slic3rConfigWrapper(ByteArrayInputStream(serialized))

        assertNotNull(reloaded.findPrint(title))
        assertEquals(title, reloaded.printConfigs.single().title)
    }
}
