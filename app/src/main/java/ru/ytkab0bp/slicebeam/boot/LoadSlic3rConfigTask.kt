package ru.ytkab0bp.slicebeam.boot

import android.util.Log
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileConfigSchemaMigrator
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.HashMap

open class LoadSlic3rConfigTask : BootTask(Runnable {
    val configFile = SliceBeam.recoverConfigFile()
    SliceBeam.getCurrentConfigFile().delete()
    if (configFile.exists()) {
        try {
            val migration = ProfileConfigSchemaMigrator.migrate(configFile)
            if (migration.migrated) {
                Log.i(
                    "LoadSlic3rConfigTask",
                    "Migrated stored profiles to schema ${migration.schemaVersion}; backup: ${migration.backupFile?.name}"
                )
            }
        } catch (e: Exception) {
            // The migration uses a backup and rollback file; keep loading the original config if it cannot upgrade.
            Log.e("LoadSlic3rConfigTask", "Stored profile migration failed; loading the existing file", e)
        }
        try {
            SliceBeam.CONFIG = Slic3rConfigWrapper(configFile)
            SliceBeam.CONFIG?.loadWarnings?.forEach { warning ->
                Log.w("LoadSlic3rConfigTask", warning.toString())
            }
            ensureKobra3V2Available()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}) {
    init {
        onWorker()
    }

    private companion object {
        private const val KOBRA_3_V2_TITLE = "Anycubic Kobra 3 v2 0.4 nozzle - Stock"

        private fun ensureKobra3V2Available() {
            val config = SliceBeam.CONFIG ?: return
            if (config.findPrinter(KOBRA_3_V2_TITLE) != null) return
            try {
                val profile = SliceBeam.requireInstance().getAssets()
                profile.open("orca_profiles/Anycubic.ini").use { anycubicIni ->
                    profile.open("orca_profiles/kobra3v2/printer/Anycubic Kobra 3 v2 0.4 nozzle - Stock.json")
                        .use { jsonInput ->
                            val bundled = Slic3rConfigWrapper(anycubicIni)
                            val base = bundled.findPrinter("Anycubic Kobra 3 0.4 nozzle") ?: return
                            val json = JSONObject(IOUtils.readString(jsonInput))
                            val v2 = IOUtils.configJsonToIni(
                                json,
                                "machine",
                                Slic3rConfigWrapper.PRINTER_CONFIG_KEYS,
                                Collections.singletonList(KOBRA_3_V2_TITLE)
                            )
                            val merged = HashMap<String, String>(base.values)
                            merged.putAll(v2.values)
                            merged.remove("inherits")
                            v2.values = merged
                            v2.put("printer_model", "Anycubic Kobra 3 V2")
                            v2.profileListType = ConfigObject.PROFILE_LIST_PRINTER
                            config.printerConfigs.add(v2)
                            if (!SliceBeam.saveConfig()) {
                                config.printerConfigs.remove(v2)
                                Log.w("LoadSlic3rConfigTask", "Bundled Kobra 3 V2 profile was not saved; removed the in-memory copy")
                            }
                        }
                }
            } catch (e: Exception) {
                Log.w("LoadSlic3rConfigTask", "Could not install bundled Kobra 3 V2 profile", e)
            }
        }
    }
}
