package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

class BeamButton(context: Context) : AppCompatTextView(context), IThemeView {
    private var colorResource = android.R.attr.colorAccent
    private var colorValue = 0

    init {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
        setPadding(ViewUtils.dp(21), 0, ViewUtils.dp(21), 0)
        onApplyTheme()
    }

    fun setColor(color: Int) {
        this.colorValue = color
        colorResource = 0
        onApplyTheme()
    }

    fun setColorRes(colorRes: Int) {
        this.colorResource = colorRes
        onApplyTheme()
    }

    override fun onApplyTheme() {
        setBackground(
            ViewUtils.createRipple(
                ThemesRepo.getColor(android.R.attr.colorControlHighlight),
                if (colorResource != 0) ThemesRepo.getColor(colorResource) else colorValue,
                16f,
            ),
        )
        setTextColor(ThemesRepo.getColor(R.attr.textColorOnAccent))
    }
}
