package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import com.mrudultora.colorpicker.ColorPickerPopUp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils

class BeamColorPickerPopUp(context: Context) : ColorPickerPopUp(context) {
    override fun show() {
        super.show()

        val window = getDialog().window ?: return
        window.setBackgroundDrawable(
            GradientDrawable().apply {
                cornerRadius = ViewUtils.dp(32).toFloat()
                setColor(ThemesRepo.getColor(R.attr.dialogBackground))
            },
        )

        val decor = window.decorView
        val titleId = resources.getIdentifier("alertTitle", "id", "android")
        if (titleId != -1) {
            decor.findViewById<TextView>(titleId)?.setTextColor(
                ThemesRepo.getColor(android.R.attr.textColorPrimary),
            )
        }

        val titleColor = ThemesRepo.getColor(android.R.attr.textColorPrimary)
        listOf(android.R.id.button1, android.R.id.button2, android.R.id.button3)
            .forEach { buttonId ->
                decor.findViewById<TextView>(buttonId)?.setTextColor(titleColor)
            }
    }
}
