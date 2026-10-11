package ru.ytkab0bp.slicebeam.theme

import android.util.SparseIntArray
import androidx.annotation.StringRes
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.utils.Prefs

class BeamTheme {
    @JvmField
    var name: String? = null

    @JvmField
    @StringRes
    var nameRes: Int = 0

    @JvmField
    var colors: SparseIntArray = SparseIntArray()

    companion object {
        @JvmField
        val LIGHT = BeamTheme().apply {
            nameRes = R.string.SettingsInterfaceThemeLight
            colors.put(R.attr.textColorOnAccent, 0xffffffff.toInt())
            colors.put(R.attr.defaultBedColor, 0xff404040.toInt())
            colors.put(R.attr.bedGridlinesColor, 0x99e5e5e5.toInt())
            colors.put(R.attr.bedContourlinesColor, 0x80ffffff.toInt())
            colors.put(R.attr.backgroundColorTop, 0xffc0c0c0.toInt())
            colors.put(R.attr.backgroundColorBottom, 0xff7a7a7a.toInt())
            colors.put(R.attr.dividerColor, 0xffeeeeee.toInt())
            colors.put(R.attr.dividerContrastColor, 0xffcccccc.toInt())
            colors.put(R.attr.dialogBackground, 0xffffffff.toInt())
            colors.put(R.attr.switchThumbUncheckedColor, 0xffeef2f3.toInt())
            colors.put(R.attr.boostyColorTop, 0xfff06e2a.toInt())
            colors.put(R.attr.boostyColorBottom, 0xff884725.toInt())
            colors.put(R.attr.telegramColor, 0xff27a7e7.toInt())
            colors.put(R.attr.k3dColor, 0xff039045.toInt())
            colors.put(R.attr.modelHoverColor, 0xffffffff.toInt())
            colors.put(R.attr.textColorNegative, 0xffff464a.toInt())

            colors.put(R.attr.gcodeViewerNone, 0xffe6b3b3.toInt())
            colors.put(R.attr.gcodeViewerPerimeter, 0xffffe64d.toInt())
            colors.put(R.attr.gcodeViewerExternalPerimeter, 0xffff7d38.toInt())
            colors.put(R.attr.gcodeViewerOverhangPerimeter, 0xff1f1fff.toInt())
            colors.put(R.attr.gcodeViewerInternalInfill, 0xffb03029.toInt())
            colors.put(R.attr.gcodeViewerSolidInfill, 0xff9654cc.toInt())
            colors.put(R.attr.gcodeViewerTopSolidInfill, 0xfff04040.toInt())
            colors.put(R.attr.gcodeViewerIroning, 0xffff8c69.toInt())
            colors.put(R.attr.gcodeViewerBridgeInfill, 0xff4d80ba.toInt())
            colors.put(R.attr.gcodeViewerGapFill, 0xffffffff.toInt())
            colors.put(R.attr.gcodeViewerSkirt, 0xff00876e.toInt())
            colors.put(R.attr.gcodeViewerSupportMaterial, 0xff00ff00.toInt())
            colors.put(R.attr.gcodeViewerSupportMaterialInterface, 0xff008000.toInt())
            colors.put(R.attr.gcodeViewerWipeTower, 0xffb3e3ab.toInt())
            colors.put(R.attr.gcodeViewerCustom, 0xff5ed194.toInt())

            colors.put(R.attr.xTrackColor, 0xffbf0000.toInt())
            colors.put(R.attr.yTrackColor, 0xff00bf00.toInt())
            colors.put(R.attr.zTrackColor, 0xff0000bf.toInt())

            colors.put(R.attr.snackbarBase, 0xffeeeeee.toInt())
            colors.put(R.attr.snackbarDone, 0xff56ab2f.toInt())
            colors.put(R.attr.snackbarWarning, 0xffae660c.toInt())
            colors.put(R.attr.snackbarInfo, 0xff009dc6.toInt())
            colors.put(R.attr.snackbarError, 0xffdc100e.toInt())

            colors.put(android.R.attr.textColorPrimary, 0xff000000.toInt())
            colors.put(android.R.attr.textColorSecondary, 0x99000000.toInt())
            colors.put(android.R.attr.windowBackground, 0xffffffff.toInt())
            colors.put(android.R.attr.colorAccent, Prefs.getAccentColor())
            colors.put(android.R.attr.colorControlHighlight, 0x21000000.toInt())
        }

        @JvmField
        val DARK = BeamTheme().apply {
            nameRes = R.string.SettingsInterfaceThemeDark
            colors = LIGHT.colors.clone()

            // OrcaSlicer dark grey palette
            colors.put(R.attr.dividerColor, 0xff3d3d3d.toInt())
            colors.put(R.attr.dividerContrastColor, 0xff484848.toInt())
            colors.put(R.attr.dialogBackground, 0xff2e2e2e.toInt())
            colors.put(R.attr.switchThumbUncheckedColor, 0xff3a3a3a.toInt())

            colors.put(R.attr.defaultBedColor, 0xff3a3a3a.toInt())
            colors.put(R.attr.bedGridlinesColor, 0x80d0d0d0.toInt())
            colors.put(R.attr.bedContourlinesColor, 0x33ffffff.toInt())
            colors.put(R.attr.backgroundColorTop, 0xff2b2b2b.toInt())
            colors.put(R.attr.backgroundColorBottom, 0xff1a1a1a.toInt())
            colors.put(R.attr.boostyColorBottom, 0xff884725.toInt())

            colors.put(R.attr.xTrackColor, 0xffee0000.toInt())
            colors.put(R.attr.yTrackColor, 0xff00ee00.toInt())
            colors.put(R.attr.zTrackColor, 0xff0000ee.toInt())

            colors.put(R.attr.snackbarBase, 0xff2d2d2d.toInt())

            colors.put(android.R.attr.textColorPrimary, 0xffffffff.toInt())
            colors.put(android.R.attr.textColorSecondary, 0x99ffffff.toInt())
            colors.put(android.R.attr.windowBackground, 0xff252525.toInt())
            colors.put(android.R.attr.colorControlHighlight, 0x21ffffff.toInt())
        }
    }
}
