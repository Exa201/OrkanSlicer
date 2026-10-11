package ru.ytkab0bp.slicebeam.events

import android.view.View
import ru.ytkab0bp.eventbus.Event
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout

@Event
open class NeedSnackbarEvent {
    @JvmField val title: CharSequence?
    @JvmField var type: SnackbarsLayout.Type = SnackbarsLayout.Type.DONE
    @JvmField var tag: String? = null
    @JvmField var buttonTitle: CharSequence? = null
    @JvmField var buttonClick: View.OnClickListener? = null

    constructor(type: SnackbarsLayout.Type, title: CharSequence?) : this(title) {
        this.type = type
    }

    constructor(title: CharSequence?) {
        this.title = title
    }

    constructor(title: Int, vararg args: Any?) {
        this.title = SliceBeam.requireInstance().getString(title, *args)
    }

    constructor(type: SnackbarsLayout.Type, title: Int, vararg args: Any?) {
        this.type = type
        this.title = SliceBeam.requireInstance().getString(title, *args)
    }

    open fun tag(tag: String?): NeedSnackbarEvent {
        this.tag = tag
        return this
    }

    open fun button(title: Int, click: View.OnClickListener?): NeedSnackbarEvent {
        buttonTitle = SliceBeam.requireInstance().getString(title)
        buttonClick = click
        return this
    }
}
