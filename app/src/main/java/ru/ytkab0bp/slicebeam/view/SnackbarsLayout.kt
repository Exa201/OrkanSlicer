package ru.ytkab0bp.slicebeam.view

import android.view.View

/** Compatibility message model used by existing event producers. Visual hosting belongs to WorkspaceSnackbarController. */
object SnackbarsLayout {
    class Snackbar(type: Type, title: CharSequence) {
        @JvmField var title: CharSequence = title
        @JvmField var type: Type = type
        @JvmField var lifetime = if (type == Type.WARNING || type == Type.ERROR) 5000 else 2500
        @JvmField var tag: String? = null
        @JvmField var buttonTitle: CharSequence? = null
        @JvmField var buttonClick: View.OnClickListener? = null

        fun tag(tag: String): Snackbar {
            lifetime = 0
            this.tag = tag
            return this
        }
    }

    enum class Type {
        DONE,
        WARNING,
        INFO,
        ERROR,
        LOADING,
    }
}
