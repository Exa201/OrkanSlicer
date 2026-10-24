package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout

/** Owns snackbar stacking, tagged replacement, dismissal, and cleanup for WorkspaceShell. */
internal class WorkspaceSnackbarController {
    private val mutableEntries = mutableStateListOf<Entry>()
    private var nextId = 0L

    /** Read from Compose; the snapshot list notifies the workspace when entries change. */
    val entries: List<Entry> get() = mutableEntries

    fun show(snackbar: SnackbarsLayout.Snackbar) {
        snackbar.tag?.let(::dismiss)
        mutableEntries += Entry(++nextId, snackbar)
    }

    /** Dismiss all entries matching a tag; null retains the existing untagged behavior. */
    fun dismiss(tag: String?) {
        mutableEntries.filter { it.snackbar.tag == tag && it.visible }.forEach { it.visible = false }
    }

    fun dismiss(id: Long) {
        mutableEntries.firstOrNull { it.id == id }?.visible = false
    }

    /** Called after the Compose exit transition completes. */
    fun remove(id: Long) {
        mutableEntries.removeAll { it.id == id }
    }

    fun clear() {
        mutableEntries.clear()
    }

    class Entry internal constructor(
        val id: Long,
        val snackbar: SnackbarsLayout.Snackbar,
    ) {
        var visible by mutableStateOf(true)
    }
}
