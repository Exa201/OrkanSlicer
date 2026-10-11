package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OCCTLoaderTest {
    @Test
    fun generatedNativeCacheUsesTheSourceBuildOpenCascadeLibraryNames() {
        val libraries = OCCTLoader.getLibraries(true)

        assertEquals("TKXDE", libraries[0])
        assertEquals("TKXDESTEP", libraries[1])
        assertFalse(libraries.contains("TKDE"))
        assertFalse(libraries.contains("TKDESTEP"))
    }

    @Test
    fun sourceBuildUsesTheSameOpenCascadeLibraryNamesAsTheCache() {
        val libraries = OCCTLoader.getLibraries(false)

        assertEquals("TKXDE", libraries[0])
        assertEquals("TKXDESTEP", libraries[1])
        assertFalse(libraries.contains("TKDE"))
        assertFalse(libraries.contains("TKDESTEP"))
        assertEquals(OCCTLoader.getLibraries(true), libraries)
    }
}
