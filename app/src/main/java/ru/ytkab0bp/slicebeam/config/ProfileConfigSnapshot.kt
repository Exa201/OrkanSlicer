package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import java.util.HashMap

/** Keeps a reversible in-memory snapshot while a group of profiles is being persisted. */
class ProfileConfigSnapshot(private val config: Slic3rConfigWrapper) {
    private val printers = snapshotList(config.printerConfigs)
    private val filaments = snapshotList(config.filamentConfigs)
    private val prints = snapshotList(config.printConfigs)
    private val physicalPrinters = snapshotList(config.physicalPrintersConfigs)
    private val printerModels = snapshotList(config.printerModels)
    private val presets = config.presets?.let(::ObjectState)
    private val vendor = config.vendor?.let(::ObjectState)

    fun restore() {
        restoreList(config.printerConfigs, printers)
        restoreList(config.filamentConfigs, filaments)
        restoreList(config.printConfigs, prints)
        restoreList(config.physicalPrintersConfigs, physicalPrinters)
        restoreList(config.printerModels, printerModels)
        config.presets = presets?.also { it.restore() }?.target
        config.vendor = vendor?.also { it.restore() }?.target
    }

    private fun snapshotList(items: List<ConfigObject>): List<ObjectState> = items.map(::ObjectState)

    private fun restoreList(target: MutableList<ConfigObject>, saved: List<ObjectState>) {
        saved.forEach(ObjectState::restore)
        target.clear()
        target.addAll(saved.map(ObjectState::target))
    }

    private class ObjectState(val target: ConfigObject) {
        private val title = target.title
        private val values = HashMap(target.values)
        private val thumbnailUrl = target.thumbnailUrl
        private val profileListType = target.profileListType

        fun restore() {
            target.setTitle(title)
            target.values.clear()
            target.values.putAll(values)
            target.thumbnailUrl = thumbnailUrl
            target.profileListType = profileListType
        }
    }
}
