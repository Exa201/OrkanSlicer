package ru.ytkab0bp.slicebeam.components.bed_menu

import ru.ytkab0bp.slicebeam.fragment.BedFragment

abstract class BedMenu {
    open fun onSetBed(fragment: BedFragment?) = Unit
}
