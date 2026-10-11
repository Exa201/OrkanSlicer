package ru.ytkab0bp.slicebeam.fragment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileListSelectionTest {
    @Test
    fun returnsTheSelectedProfileAndKeepsListOrder() {
        val items = listOf(
            ProfileRow("Preset A", selected = false),
            ProfileRow("Preset B", selected = true),
            ProfileRow("Preset C", selected = true),
        )

        assertEquals("Preset B", ProfileListSelection.selected(items)?.title)
    }

    @Test
    fun returnsNullWhenNoProfileIsSelected() {
        assertNull(ProfileListSelection.selected(listOf(ProfileRow("Preset A", selected = false))))
    }

    private data class ProfileRow(
        override val title: String?,
        private val selected: Boolean,
    ) : ProfileListItem {
        override fun isSelected(): Boolean = selected
    }
}
