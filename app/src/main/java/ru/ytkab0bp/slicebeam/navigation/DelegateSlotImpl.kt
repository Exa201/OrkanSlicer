package ru.ytkab0bp.slicebeam.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.FilamentConfigFragment
import ru.ytkab0bp.slicebeam.fragment.MakerFragment
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.fragment.PrinterConfigFragment
import ru.ytkab0bp.slicebeam.fragment.SettingsFragment

/** Maps the six screen stacks to their titles, icons, and screen implementations. */
abstract class DelegateSlotImpl : NavigationDelegate() {
    open fun getSlotCount(): Int = 6

    @DrawableRes
    open fun getSlotIcon(slot: Int): Int = when (slot) {
        1 -> R.drawable.menu3d_smart_slice
        2 -> R.drawable.menu3d_layers
        3 -> R.drawable.menu3d_spool
        4 -> R.drawable.menu3d_printer
        5 -> R.drawable.menu3d_tools
        else -> R.drawable.menu3d_objects
    }

    open fun needDisplaySlotGear(slot: Int): Boolean = slot != 0 && slot != 5

    @StringRes
    open fun getSlotTitle(slot: Int): Int = when (slot) {
        1 -> R.string.SlotMaker
        2 -> R.string.SlotPrintConfig
        3 -> R.string.SlotFilamentConfig
        4 -> R.string.SlotPrinterConfig
        5 -> R.string.SlotAppSettings
        else -> R.string.SlotBed
    }

    /** Short labels keep the phone navigation readable at the app's larger text setting. */
    @StringRes
    open fun getSlotNavigationTitle(slot: Int): Int = when (slot) {
        0 -> R.string.NavigationPreview
        3 -> R.string.NavigationFilament
        5 -> R.string.NavigationApp
        else -> getSlotTitle(slot)
    }

    @StringRes
    open fun getSlotTooltip(slot: Int): Int = when (slot) {
        1 -> R.string.SlotMakerTooltip
        2 -> R.string.SlotPrintConfigTooltip
        3 -> R.string.SlotFilamentConfigTooltip
        4 -> R.string.SlotPrinterConfigTooltip
        5 -> R.string.SlotAppSettingsTooltip
        else -> getSlotTitle(slot)
    }

    override fun newFragment(slot: Int): Fragment = when (slot) {
        1 -> MakerFragment()
        2 -> PrintConfigFragment()
        3 -> FilamentConfigFragment()
        4 -> PrinterConfigFragment()
        5 -> SettingsFragment()
        else -> BedFragment()
    }
}
