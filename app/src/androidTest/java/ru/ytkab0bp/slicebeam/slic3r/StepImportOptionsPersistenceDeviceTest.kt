package ru.ytkab0bp.slicebeam.slic3r

import android.app.Application
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.utils.Prefs

@RunWith(AndroidJUnit4::class)
class StepImportOptionsPersistenceDeviceTest {
    @Test fun defaultsSplitAssembliesButRespectAnExplicitSavedChoice() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Prefs.init(application)
        val preferences = PreferenceManager.getDefaultSharedPreferences(application)
        val keys = listOf("step_linear_deflection", "step_angular_deflection", "step_split_compound")
        val original = keys.associateWith { key ->
            if (preferences.contains(key)) preferences.all[key] else null
        }
        val originallyPresent = keys.filterTo(mutableSetOf()) { preferences.contains(it) }

        try {
            preferences.edit().also { editor -> keys.forEach(editor::remove) }.commit()
            assertTrue("a fresh install should split STEP assemblies by default", StepImportOptions.loadDefaults().splitCompound)

            StepImportOptions.saveDefaults(StepImportOptions(splitCompound = false))
            assertFalse("an explicitly saved choice must override Orca's new default", StepImportOptions.loadDefaults().splitCompound)

            StepImportOptions.saveDefaults(StepImportOptions(splitCompound = true))
            assertTrue("a saved opt-in must remain enabled on later imports", StepImportOptions.loadDefaults().splitCompound)
        } finally {
            preferences.edit().also { editor ->
                keys.forEach { key ->
                    if (key !in originallyPresent) editor.remove(key)
                    else when (val value = original[key]) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                    }
                }
            }.commit()
        }
    }
}
