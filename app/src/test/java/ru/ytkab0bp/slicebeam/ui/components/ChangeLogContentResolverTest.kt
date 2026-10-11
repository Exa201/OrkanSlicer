package ru.ytkab0bp.slicebeam.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeLogContentResolverTest {
    @Test
    fun releaseNotesUseTheDeviceLanguageAndFallBackToEnglish() {
        val notes = mapOf("de" to "Änderungen", "en" to "Changes")

        assertEquals("Änderungen", ChangeLogContentResolver.localizedText("de", notes))
        assertEquals("Changes", ChangeLogContentResolver.localizedText("ru", notes))
        assertEquals("", ChangeLogContentResolver.localizedText("ru", emptyMap()))
    }

    @Test
    fun supporterPageIsOnlyIncludedWhenAvailable() {
        assertEquals(1, ChangeLogContentResolver.pageCount(hasSupporters = false))
        assertEquals(2, ChangeLogContentResolver.pageCount(hasSupporters = true))
    }

    @Test
    fun finalPageClosesInsteadOfAdvancing() {
        assertFalse(ChangeLogContentResolver.isLastPage(currentPage = 0, pageCount = 2))
        assertTrue(ChangeLogContentResolver.isLastPage(currentPage = 1, pageCount = 2))
        assertTrue(ChangeLogContentResolver.isLastPage(currentPage = 0, pageCount = 1))
    }
}
