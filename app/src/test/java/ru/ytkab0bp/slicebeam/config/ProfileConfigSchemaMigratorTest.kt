package ru.ytkab0bp.slicebeam.config

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

class ProfileConfigSchemaMigratorTest {
    @Test
    fun migratesLegacyProfileKeysAndKeepsUnknownConfigTextWithRecoverableBackup() {
        val directory = Files.createTempDirectory("profile-schema-migration").toFile()
        try {
            val file = File(directory, "slic3r.ini")
            val original = listOf(
                "# local profile bundle",
                "[filament:Old PLA]",
                "bed_temperature = 60",
                "[print:Old process]",
                "perimeters = 3",
                "wall_filament = 1",
                "[future_vendor_section:opaque]",
                "wall_filament = vendor-value",
                "opaque_key = keep this"
            ).joinToString("\r\n", postfix = "\r\n")
            file.writeText(original, StandardCharsets.UTF_8)

            val result = ProfileConfigSchemaMigrator.migrate(file)
            val migrated = file.readText(StandardCharsets.UTF_8)

            assertTrue(result.migrated)
            assertEquals(ProfileConfigSchemaMigrator.CURRENT_SCHEMA_VERSION, result.schemaVersion)
            assertNotNull(result.backupFile)
            assertEquals(original, result.backupFile!!.readText(StandardCharsets.UTF_8))
            assertTrue(migrated.startsWith("# slicebeam_profile_schema=1\r\n"))
            assertTrue(migrated.contains("hot_plate_temp = 60\r\n"))
            assertTrue(migrated.contains("wall_loops = 3\r\n"))
            assertTrue(migrated.contains("outer_wall_filament_id = 0\r\n"))
            assertTrue(migrated.contains("[future_vendor_section:opaque]\r\nwall_filament = vendor-value\r\n"))
            assertTrue(migrated.contains("opaque_key = keep this\r\n"))
            assertFalse(migrated.contains("perimeters = 3"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun currentSchemaIsIdempotentAndDoesNotCreateAnotherBackup() {
        val directory = Files.createTempDirectory("profile-schema-current").toFile()
        try {
            val file = File(directory, "slic3r.ini")
            val original = "# slicebeam_profile_schema=1\n[print:Current]\nwall_loops = 4\n"
            file.writeText(original, StandardCharsets.UTF_8)

            val result = ProfileConfigSchemaMigrator.migrate(file)

            assertFalse(result.migrated)
            assertEquals(1, result.schemaVersion)
            assertEquals(original, file.readText(StandardCharsets.UTF_8))
            assertFalse(File(directory, "slic3r.ini.schema-1.bak").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun futureSchemaIsNeverDowngraded() {
        val directory = Files.createTempDirectory("profile-schema-future").toFile()
        try {
            val file = File(directory, "slic3r.ini")
            val original = "# slicebeam_profile_schema=7\n[print:Future]\nwall_loops = 5\n"
            file.writeText(original, StandardCharsets.UTF_8)

            val result = ProfileConfigSchemaMigrator.migrate(file)

            assertFalse(result.migrated)
            assertEquals(7, result.schemaVersion)
            assertEquals(original, file.readText(StandardCharsets.UTF_8))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun restoresInterruptedReplacementBeforeStartingMigration() {
        val directory = Files.createTempDirectory("profile-schema-recovery").toFile()
        try {
            val file = File(directory, "slic3r.ini")
            val recovery = File(directory, "slic3r.ini.schema-migration.rollback")
            val original = "[print:Old]\nperimeters = 2\n"
            recovery.writeText(original, StandardCharsets.UTF_8)

            val result = ProfileConfigSchemaMigrator.migrate(file)

            assertTrue(result.migrated)
            assertTrue(file.readText(StandardCharsets.UTF_8).contains("wall_loops = 2"))
            assertEquals(original, result.backupFile!!.readText(StandardCharsets.UTF_8))
            assertFalse(recovery.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun migratedProfilesLoadAndUnknownSectionsCannotLeakIntoPreviousProfile() {
        val directory = Files.createTempDirectory("profile-schema-parse").toFile()
        try {
            val file = File(directory, "slic3r.ini")
            file.writeText(
                """|[print:Child]
                    |inherits = Missing base
                    |perimeters = 2
                    |future_or_unsupported_option = 1
                    |[unknown:vendor]
                    |wall_loops = 99
                    |[print:Other]
                    |wall_loops = 4
                """.trimMargin() + "\n",
                StandardCharsets.UTF_8
            )

            ProfileConfigSchemaMigrator.migrate(file)
            val loaded = Slic3rConfigWrapper(
                ByteArrayInputStream(file.readBytes())
            )

            assertEquals("2", loaded.findPrint("Child")?.get("wall_loops"))
            assertEquals("4", loaded.findPrint("Other")?.get("wall_loops"))
            assertTrue(
                loaded.loadWarnings.any {
                    it is ProfileConfigLoadWarning.MissingParent && it.parent == "Missing base"
                }
            )
            assertTrue(
                loaded.loadWarnings.any {
                    it is ProfileConfigLoadWarning.UnknownOption &&
                        it.profile == "Child" && it.key == "future_or_unsupported_option"
                }
            )
            assertTrue(loaded.printConfigs.none { it.title == null })
        } finally {
            directory.deleteRecursively()
        }
    }
}
