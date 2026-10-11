package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedMenuItemTest {
    @Test
    fun actionModelKeepsEnableToggleAndSectionStateWithoutCreatingAndroidViews() {
        var toggledTo: Boolean? = null
        val item = BedMenuItem(1, 2)
            .setEnabled(false)
            .setCheckable(BedMenuItemCheckedChangeListener { toggledTo = it }, true)
            .setSectionBreakBefore()

        assertFalse(item.isEnabled)
        assertTrue(item.isCheckable)
        assertTrue(item.isChecked)
        assertTrue(item.sectionBreakBefore)

        item.checkedChangeListener?.onCheckedChanged(false)
        assertFalse(toggledTo!!)
    }
}
