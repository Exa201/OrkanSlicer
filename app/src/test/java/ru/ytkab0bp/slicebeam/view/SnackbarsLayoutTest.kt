package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceSnackbarController

class SnackbarsLayoutTest {
    @Test
    fun aNewSnackbarWithTheSameTagDismissesThePreviousEntry() {
        val controller = WorkspaceSnackbarController()
        val previous = SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Importing").tag("import")
        val replacement = SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Cancelling").tag("import")

        controller.show(previous)
        controller.show(replacement)

        assertEquals(2, controller.entries.size)
        assertFalse(controller.entries[0].visible)
        assertTrue(controller.entries[1].visible)
        assertEquals("import", controller.entries[1].snackbar.tag)
    }

    @Test
    fun nullTagDismissesOnlyUntaggedEntries() {
        val controller = WorkspaceSnackbarController()
        controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.INFO, "Untagged"))
        controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.LOADING, "Tagged").tag("task"))

        controller.dismiss(null)

        assertFalse(controller.entries[0].visible)
        assertTrue(controller.entries[1].visible)
    }

    @Test
    fun removalAndClearReleaseDismissedAndVisibleEntries() {
        val controller = WorkspaceSnackbarController()
        controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.INFO, "First"))
        controller.show(SnackbarsLayout.Snackbar(SnackbarsLayout.Type.INFO, "Second"))
        val firstId = controller.entries.first().id

        controller.dismiss(firstId)
        controller.remove(firstId)

        assertEquals(listOf("Second"), controller.entries.map { it.snackbar.title.toString() })
        controller.clear()
        assertTrue(controller.entries.isEmpty())
    }
}
