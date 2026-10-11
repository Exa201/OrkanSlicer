package ru.ytkab0bp.slicebeam.config

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/** Performs a recoverable, line-preserving upgrade of the app's persisted profile bundle. */
object ProfileConfigSchemaMigrator {
    const val CURRENT_SCHEMA_VERSION = 1

    private const val MARKER_PREFIX = "# slicebeam_profile_schema="
    private const val ROLLBACK_SUFFIX = ".schema-migration.rollback"

    data class Result(
        val migrated: Boolean,
        val schemaVersion: Int,
        val backupFile: File? = null,
    )

    /**
     * Upgrade known legacy Orca keys in persisted profile sections. Unknown sections, keys,
     * comments, and line endings are retained verbatim. A byte-for-byte backup is made before
     * replacing the source file.
     */
    @Throws(IOException::class)
    fun migrate(file: File): Result {
        recoverInterruptedReplacement(file)
        if (!file.isFile) return Result(migrated = false, schemaVersion = CURRENT_SCHEMA_VERSION)

        val original = readUtf8(file)
        val existingVersion = readSchemaVersion(original)
        if (existingVersion >= CURRENT_SCHEMA_VERSION) {
            return Result(migrated = false, schemaVersion = existingVersion)
        }

        val upgraded = upgradeText(original)
        val backup = createBackup(file, original, existingVersion)
        replaceWithRollback(file, upgraded)
        return Result(migrated = true, schemaVersion = CURRENT_SCHEMA_VERSION, backupFile = backup)
    }

    private fun readSchemaVersion(text: String): Int {
        for (line in text.lineSequence()) {
            val normalized = line.removePrefix("\uFEFF").trim()
            if (normalized.startsWith(MARKER_PREFIX)) {
                return normalized.removePrefix(MARKER_PREFIX).toIntOrNull() ?: 0
            }
        }
        return 0
    }

    private fun upgradeText(source: String): String {
        val output = StringBuilder(source.length + 48)
        val newline = if (source.contains("\r\n")) "\r\n" else "\n"
        var position = 0
        var profileSection = false
        var markerWritten = false

        while (position < source.length) {
            val newlineAt = source.indexOf('\n', position)
            val hasNewline = newlineAt >= 0
            val end = if (hasNewline) newlineAt else source.length
            val rawLine = source.substring(position, end)
            val hasCarriageReturn = rawLine.endsWith('\r')
            val line = if (hasCarriageReturn) rawLine.dropLast(1) else rawLine
            val markerLine = line.removePrefix("\uFEFF").trim().startsWith(MARKER_PREFIX)

            val upgradedLine = when {
                markerLine -> {
                    markerWritten = true
                    line.substringBefore(MARKER_PREFIX) + MARKER_PREFIX + CURRENT_SCHEMA_VERSION
                }
                line.startsWith("[") && line.endsWith("]") -> {
                    val category = line.substring(1, line.length - 1).substringBefore(':')
                    profileSection = category == "print" || category == "printer" ||
                        category == "filament" || category == "physical_printer"
                    line
                }
                profileSection -> migrateProfileEntry(line)
                else -> line
            }

            output.append(upgradedLine)
            if (hasCarriageReturn) output.append('\r')
            if (hasNewline) output.append('\n')
            position = if (hasNewline) newlineAt + 1 else source.length
        }

        if (!markerWritten) {
            val bom = if (output.isNotEmpty() && output[0] == '\uFEFF') "\uFEFF" else ""
            if (bom.isNotEmpty()) output.deleteCharAt(0)
            output.insert(0, "$MARKER_PREFIX$CURRENT_SCHEMA_VERSION$newline")
            if (bom.isNotEmpty()) output.insert(0, bom)
        }
        return output.toString()
    }

    private fun migrateProfileEntry(line: String): String {
        val separator = line.indexOf(" = ")
        if (separator <= 0) return line

        val rawKey = line.substring(0, separator)
        val key = rawKey.trim()
        val currentKey = ConfigObject.migrateKey(key)
        if (currentKey == key) return line

        val rawValue = line.substring(separator + 3)
        val parsedValue = rawValue.trim { it <= ' ' }.replace("\\n", "\n")
        val normalized = ConfigObject().apply { put(key, parsedValue) }[currentKey] ?: parsedValue
        val serializedValue = normalized.replace("\n", "\\n")
        val leadingWhitespace = rawKey.takeWhile { it == ' ' || it == '\t' }
        return "$leadingWhitespace$currentKey = $serializedValue"
    }

    private fun createBackup(file: File, original: String, schemaVersion: Int): File {
        val parent = file.absoluteFile.parentFile ?: throw IOException("Profile file has no parent directory")
        val stem = "${file.name}.schema-$schemaVersion.bak"
        var candidate = File(parent, stem)
        var suffix = 1
        while (candidate.exists() && !readUtf8(candidate).contentEquals(original)) {
            candidate = File(parent, "$stem.$suffix")
            suffix++
        }
        if (!candidate.exists()) {
            try {
                candidate.createNewFile()
                FileInputStream(file).use { input ->
                    FileOutputStream(candidate, false).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                }
            } catch (error: Exception) {
                candidate.delete()
                if (error is IOException) throw error
                throw IOException("Could not back up the profile file", error)
            }
        }
        return candidate
    }

    private fun replaceWithRollback(file: File, contents: String) {
        val parent = file.absoluteFile.parentFile ?: throw IOException("Profile file has no parent directory")
        val rollback = File(parent, file.name + ROLLBACK_SUFFIX)
        val staged = File(parent, file.name + ".schema-migration.tmp")
        if (rollback.exists()) throw IOException("A previous profile migration recovery file still exists")
        if (staged.exists() && !staged.delete()) throw IOException("Could not clear a stale profile migration file")

        try {
            FileOutputStream(staged, false).use { output ->
                output.write(contents.toByteArray(StandardCharsets.UTF_8))
                output.fd.sync()
            }
            if (!file.renameTo(rollback)) throw IOException("Could not stage the current profile file")
            if (!staged.renameTo(file)) {
                rollback.renameTo(file)
                throw IOException("Could not install the migrated profile file")
            }
            if (!rollback.delete()) {
                // The installed file is already complete; the durable schema backup remains available.
                rollback.deleteOnExit()
            }
        } finally {
            if (staged.exists()) staged.delete()
        }
    }

    private fun recoverInterruptedReplacement(file: File) {
        val parent = file.absoluteFile.parentFile ?: return
        val rollback = File(parent, file.name + ROLLBACK_SUFFIX)
        if (!file.exists() && rollback.exists()) {
            if (!rollback.renameTo(file)) {
                throw IOException("Could not restore the profile file after an interrupted migration")
            }
        } else if (file.exists() && rollback.exists()) {
            rollback.delete()
        }
    }

    private fun readUtf8(file: File): String =
        file.readText(StandardCharsets.UTF_8)
}
