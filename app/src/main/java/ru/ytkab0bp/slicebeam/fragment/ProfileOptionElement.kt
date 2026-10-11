package ru.ytkab0bp.slicebeam.fragment

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ru.ytkab0bp.slicebeam.recycler.SpaceItem
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import java.util.function.Consumer
import java.util.function.IntConsumer
import java.util.function.Supplier

/** Option-row state and presenter selection shared by every profile screen. */
open class ProfileOptionElement(
    context: Context?,
    definition: ConfigOptionDef?,
    private val elementIndex: Int,
    private val valueProvider: Supplier<String>?,
    private val onChanged: Consumer<String>?,
    private val refreshRow: IntConsumer?,
    private val onLongClick: Runnable?,
) {
    @JvmField
    var visibleInAdvanced: Boolean = false

    @JvmField
    var specialType: Int = -1

    @JvmField
    var icon: Int = 0

    @JvmField
    var title: String? = null

    @JvmField
    var color: Int = 0

    @JvmField
    var noTint: Boolean = false

    @JvmField
    var simpleItem: ProfileSettingsComposeItem? = null

    @JvmField
    var optionDef: ConfigOptionDef? = definition

    @JvmField
    var onClick: Runnable? = null

    private var boundIndex = -1

    fun usesDirectCompose(): Boolean =
        optionDef?.let(ProfileOptionComposePresenter::supportsDirect) == true

    internal fun saveableRowIdentity(): String? = optionDef?.key
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it:$elementIndex" }

    @Composable
    fun DirectContent(
        bindVersion: Int,
        rowPosition: Int,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) {
        val definition = optionDef ?: return
        // Do not key the row by its bind version: it changes when the profile page is rebuilt
        // after rotation and would discard an open editor dialog and its unsaved text draft.
        ProfileOptionComposePresenter.DirectContent(
            context = LocalContext.current,
            def = definition,
            elementIndex = elementIndex,
            initialValue = valueProvider?.get().orEmpty(),
            onChanged = Consumer { value ->
                onChanged?.accept(value)
                refreshRow?.accept(rowPosition)
            },
            onLongClick = Runnable { onLongClick?.run() },
            onEditorStateChanged = onEditorStateChanged,
        )
    }

    init {
        simpleItem = createSimpleItem(context, definition)
    }

    private fun createSimpleItem(context: Context?, definition: ConfigOptionDef?): ProfileSettingsComposeItem? {
        if (definition == null) return if (context != null) SpaceItem(0, 0) else null
        if (ProfileOptionComposePresenter.supportsDirect(definition)) return null
        val rowContext = context ?: return null
        val valueSource = valueProvider ?: Supplier { "" }
        val changed = Consumer<String> { value ->
            onChanged?.accept(value)
            refreshBoundRow()
        }
        return ProfileOptionComposePresenter.create(
            rowContext,
            definition,
            elementIndex,
            valueSource,
            changed,
            Runnable { onLongClick?.run() },
        )
    }

    constructor(specialType: Int) : this(null, null, -1, null, null, null, null) {
        this.specialType = specialType
    }

    constructor(icon: Int, title: String) : this(null, null, -1, null, null, null, null) {
        this.icon = icon
        this.title = title
    }

    constructor(item: ProfileSettingsComposeItem) : this(null, null, -1, null, null, null, null) {
        simpleItem = item
    }

    fun bindRowPosition(position: Int) {
        boundIndex = position
    }

    open fun setVisibleInAdvanced(): ProfileOptionElement {
        visibleInAdvanced = true
        return this
    }

    open fun setOnClick(onClick: Runnable?): ProfileOptionElement {
        this.onClick = onClick
        return this
    }

    open fun setColor(color: Int, noTint: Boolean): ProfileOptionElement {
        this.color = color
        this.noTint = noTint
        return this
    }

    private fun refreshBoundRow() {
        if (boundIndex >= 0) refreshRow?.accept(boundIndex)
    }
}
