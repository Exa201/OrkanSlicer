package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDropdownMenuPolicyTest {
    @Test
    fun multipleProfilesKeepSelectionAndAllowDeletion() {
        val menu = ProfileDropdownMenuPolicy.create(listOf("PLA", "PETG", "ABS"), selectedIndex = 1)

        assertEquals(listOf("PLA", "PETG", "ABS"), menu.titles)
        assertEquals(1, menu.selectedIndex)
        assertTrue(menu.canDelete)
    }

    @Test
    fun onlyRemainingProfileCannotBeDeleted() {
        val menu = ProfileDropdownMenuPolicy.create(listOf("Standard"), selectedIndex = 0)

        assertEquals(0, menu.selectedIndex)
        assertFalse(menu.canDelete)
    }

    @Test
    fun emptyProfileListHasNoSelectionOrDeleteAction() {
        val menu = ProfileDropdownMenuPolicy.create(emptyList(), selectedIndex = 0)

        assertEquals(-1, menu.selectedIndex)
        assertFalse(menu.canDelete)
        assertNull(ProfileDropdownMenuPolicy.selectedIndex(menu, 0))
    }

    @Test
    fun staleSelectionIndexIsIgnored() {
        val menu = ProfileDropdownMenuPolicy.create(listOf("PLA", "PETG"), selectedIndex = 8)

        assertEquals(-1, menu.selectedIndex)
        assertEquals(1, ProfileDropdownMenuPolicy.selectedIndex(menu, 1))
        assertNull(ProfileDropdownMenuPolicy.selectedIndex(menu, 2))
    }
}
