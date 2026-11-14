package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.SliceBeam
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.Locale
import java.util.regex.Pattern

/** Loads Orca's PO strings and applies the app name without changing native message IDs. */
open class Slic3rLocalization private constructor(strings: Map<String, String>) {
    private val translations = HashMap(strings)

    @Throws(IOException::class)
    constructor(key: String) : this(
        parsePo(SliceBeam.requireInstance().assets.open("localization/$key.po"))
    )

    open fun get(key: String): String {
        val translated = translations[key]
        if (!translated.isNullOrEmpty()) return translated
        return replaceString(key).also { translations[key] = it }
    }

    companion object {
        private val localesMap = HashMap<String, Slic3rLocalization>()
        private val escapedCharacterPattern = Pattern.compile("\\\\(.)")
        private val slic3rNamePattern = Pattern.compile("(?i)slic3r")
        private val prusaSlicerNamePattern = Pattern.compile("(?i)prusasl?i[sc]er")

        @JvmStatic
        fun getString(key: String): String = getInstance().get(key)

        @JvmStatic
        fun getInstance(key: String): Slic3rLocalization = synchronized(localesMap) {
            localesMap[key]?.let { return@synchronized it }
            val locale = try {
                Slic3rLocalization(key)
            } catch (error: IOException) {
                error.printStackTrace()
                try {
                    Slic3rLocalization("en")
                } catch (fallbackError: IOException) {
                    throw RuntimeException(fallbackError)
                }
            }
            localesMap[key] = locale
            locale
        }

        @JvmStatic
        fun getInstance(): Slic3rLocalization = getInstance(
            OrcaLanguage.byTag(ru.ytkab0bp.slicebeam.utils.Prefs.getPrefs()
                ?.getString("app_language", Locale.getDefault().language)).assetKey,
        )

        /** The same parser entry used by the asset loader, exposed internally for JVM tests. */
        internal fun fromPo(input: InputStream): Slic3rLocalization = Slic3rLocalization(parsePo(input))

        private fun parsePo(input: InputStream): Map<String, String> {
            val result = HashMap<String, String>()
            var messageId: StringBuilder? = null
            var pluralMessageId: StringBuilder? = null
            var messageString: StringBuilder? = null
            val pluralTranslations = HashMap<Int, StringBuilder>()
            var activePluralTranslation: StringBuilder? = null
            var activeField: Field = Field.NONE

            fun flushEntry() {
                val id = messageId?.toString()
                val pluralId = pluralMessageId?.toString()
                val translation = messageString?.toString()
                if (!id.isNullOrEmpty()) {
                    val translatedSingular = translation?.takeIf { it.isNotEmpty() }
                        ?: pluralTranslations.toSortedMap().values.firstOrNull { it.isNotEmpty() }?.toString()
                    if (!translatedSingular.isNullOrEmpty()) result[id] = replaceString(translatedSingular)
                }
                if (!pluralId.isNullOrEmpty() && pluralTranslations.isNotEmpty()) {
                    val translatedPlural = pluralTranslations.toSortedMap().entries
                        .lastOrNull { it.value.isNotEmpty() }
                        ?.value
                        ?.toString()
                    if (!translatedPlural.isNullOrEmpty()) result[pluralId] = replaceString(translatedPlural)
                }
                messageId = null
                pluralMessageId = null
                messageString = null
                pluralTranslations.clear()
                activePluralTranslation = null
                activeField = Field.NONE
            }

            input.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    when {
                        line.startsWith("#") -> Unit
                        line.startsWith("msgid_plural") -> {
                            pluralMessageId = StringBuilder(line.substringAfter('"').substringBeforeLast('"'))
                            activeField = Field.PLURAL_ID
                        }
                        line.startsWith("msgid") -> {
                            messageId = StringBuilder(line.substring(7, line.length - 1))
                            activeField = Field.ID
                        }
                        line.startsWith("msgstr[") -> {
                            val index = line.substringAfter('[').substringBefore(']').toIntOrNull()
                            activePluralTranslation = if (index != null) {
                                StringBuilder(line.substringAfter('"').substringBeforeLast('"')).also { pluralTranslations[index] = it }
                            } else null
                            messageString = null
                            activeField = Field.PLURAL_TRANSLATION
                        }
                        line.startsWith("msgstr") -> {
                            messageString = StringBuilder(line.substring(8, line.length - 1))
                            activePluralTranslation = null
                            activeField = Field.TRANSLATION
                        }
                        line.isEmpty() -> flushEntry()
                        line.startsWith('"') && line.endsWith('"') -> {
                            val continuation = line.substring(1, line.length - 1)
                            when (activeField) {
                                Field.ID -> messageId?.append(continuation)
                                Field.PLURAL_ID -> pluralMessageId?.append(continuation)
                                Field.TRANSLATION -> messageString?.append(continuation)
                                Field.PLURAL_TRANSLATION -> activePluralTranslation?.append(continuation)
                                Field.NONE -> Unit
                            }
                        }
                    }
                }
                // Some valid PO files end at the final quote without a separator line.
                flushEntry()
            }
            return result
        }

        private enum class Field { NONE, ID, PLURAL_ID, TRANSLATION, PLURAL_TRANSLATION }

        private fun replaceString(value: String?): String {
            if (value == null) return ""
            val escaped = escapedCharacterPattern.matcher(value.replace("\\n", "\n")).replaceAll("\$1")
            val slic3rReplaced = slic3rNamePattern.matcher(escaped).replaceAll("OrkanSlicer")
            return prusaSlicerNamePattern.matcher(slic3rReplaced).replaceAll("OrkanSlicer")
        }
    }
}
