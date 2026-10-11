package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

class ProfileNameValidatorTest {
    @Test
    fun acceptsNormalNamesAndRejectsBlankNames() {
        assertTrue(ProfileNameValidator.isValid("PLA 0.20 mm – Galal"))
        assertFalse(ProfileNameValidator.isValid(""))
        assertFalse(ProfileNameValidator.isValid("   "))
    }

    @Test
    fun rejectsEveryCharacterForbiddenByOrcaConfigWrapper() {
        Slic3rConfigWrapper.BLACKLISTED_SYMBOLS.forEach { character ->
            assertFalse("Expected '$character' to be rejected", ProfileNameValidator.isValid("PLA$character profile"))
        }
    }

    @Test
    fun detectsDuplicateNamesWithoutRejectingTheCurrentProfileName() {
        val names = listOf("Printer A", "PETG", null)
        assertTrue(ProfileNameValidator.conflictsWithExisting("petg", names, "PLA"))
        assertFalse(ProfileNameValidator.conflictsWithExisting("printer a", names, "Printer A"))
        assertFalse(ProfileNameValidator.conflictsWithExisting("PLA", names, "PLA"))
    }
}
