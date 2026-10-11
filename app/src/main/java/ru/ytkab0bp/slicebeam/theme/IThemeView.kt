package ru.ytkab0bp.slicebeam.theme

/** Contract for legacy Android views that refresh colors after an app theme change. */
interface IThemeView {
    fun onApplyTheme()
}
