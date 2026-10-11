package ru.ytkab0bp.slicebeam.fragment

/** Adapter model that presents either one profile option or a category heading. */
class ProfileOptionWrapper(
    @JvmField var icon: Int,
    @JvmField var title: String?,
    @JvmField var onClick: Runnable?,
    @JvmField var color: Int,
    @JvmField var noTint: Boolean,
) {
    @JvmField
    var optionEl: ProfileOptionElement? = null

    @JvmField
    var categoryIndex: Int = 0

    /** Accordion headings may own an explicit expansion callback instead of a row insertion. */
    @JvmField
    var isExpandable: Boolean = false

    @JvmField
    var isExpanded: Boolean = false

    constructor(option: ProfileOptionElement) : this(0, null, null, 0, false) {
        optionEl = option
    }

    fun hasSpecialType(): Boolean = optionEl?.specialType?.let { it != -1 } ?: false

    companion object {
        @JvmStatic
        fun fromOption(element: ProfileOptionElement): ProfileOptionWrapper {
            val wrapper = if (element.title != null) {
                ProfileOptionWrapper(element.icon, element.title, element.onClick, element.color, element.noTint)
            } else {
                ProfileOptionWrapper(element)
            }
            if (element.specialType != -1) {
                wrapper.color = element.color
                wrapper.noTint = element.noTint
                wrapper.onClick = element.onClick
            }
            return wrapper
        }
    }
}
