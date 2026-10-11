package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Test

class BedMenuItemCheckedChangeListenerTest {
    @Test
    fun composeToggleCallbackReceivesItsNewCheckedState() {
        var receivedCheckedState: Boolean? = null
        val listener = BedMenuItemCheckedChangeListener { checked ->
            receivedCheckedState = checked
        }

        listener.onCheckedChanged(true)

        assertEquals(true, receivedCheckedState)
    }
}
