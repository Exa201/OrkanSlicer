package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import ru.ytkab0bp.slicebeam.fragment.ProfileListItem

class ConfigObjectProfileListItemTest {
    @Test
    fun exposesProfileTitleAndSelectionThroughTheKotlinContract() {
        val profile = ConfigObject("Draft").apply { profileListType = -1 }
        val item: ProfileListItem = profile

        assertEquals("Draft", item.title)
        assertFalse(item.isSelected())
    }
}
