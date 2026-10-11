package ru.ytkab0bp.slicebeam.fragment

import ru.ytkab0bp.slicebeam.recycler.SpaceItem
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaSettingsCategory
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Builds process-profile rows from the Kotlin category catalog and Orca's native option schema. */
object OrcaPrintSettingsBuilder {
    @JvmStatic
    fun build(fragment: PrintConfigFragment): List<ProfileOptionElement> {
        val definition = PrintConfigDef.getInstance()
        val addedKeys = HashSet<String>()
        val items = ArrayList<ProfileOptionElement>()

        OrcaProcessSettingsCatalog.categories().forEach { category ->
            items.addAll(buildCategory(fragment, definition, addedKeys, category))
        }
        return items
    }

    private fun buildCategory(
        fragment: PrintConfigFragment,
        definition: PrintConfigDef,
        addedKeys: MutableSet<String>,
        category: OrcaSettingsCategory,
    ): List<ProfileOptionElement> {
        val items = ArrayList<ProfileOptionElement>()
        items.add(ProfileOptionElement(category.iconResId, category.title))

        category.sections.forEach { section ->
            items.add(ProfileOptionElement(ProfileSubHeader(section.title)))
            section.keys.forEach { key ->
                val option = definition.options[key]
                // Keep a bad catalog edit from showing one Orca setting twice.
                if (option != null && addedKeys.add(key)) {
                    items.add(fragment.createOptionElement(option))
                }
            }
            items.add(ProfileOptionElement(SpaceItem(0, ViewUtils.dp(4f))))
        }
        return items
    }
}
