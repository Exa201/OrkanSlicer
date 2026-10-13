package ru.ytkab0bp.slicebeam.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets

/** Some printer APIs return an error object with HTTP 200; never report those as started prints. */
object PrinterResponseInspector {
    private const val MAX_DETAIL_CHARS = 500

    /** A web page from an API endpoint usually means the host type, port or proxy is wrong. */
    @JvmStatic
    fun missingAcknowledgement(body: ByteArray?, fallback: String): String {
        val prefix = body?.toString(StandardCharsets.UTF_8)?.trimStart()?.take(256)?.lowercase().orEmpty()
        return if (prefix.startsWith("<!doctype html") || prefix.startsWith("<html"))
            "Die Druckeradresse liefert eine Webseite statt einer API-Antwort. Hosttyp und Port prüfen; es wurde kein Druck gestartet."
        else fallback
    }

    @JvmStatic
    fun embeddedError(body: ByteArray?): String? {
        val root = parseObject(body) ?: return null
        val error = root.get("error")
        if (error != null && !error.isJsonNull && scalar(error)?.trim()?.lowercase() !in setOf("0", "false", "")) {
            val errorObject = error.takeIf { it.isJsonObject }?.asJsonObject
            val message = if (errorObject != null) message(errorObject) else scalar(error)
            val code = errorObject?.get("code")?.let(::scalar)
            if (code?.trim() != "0") {
                return format(message, code) ?: "Der Drucker hat einen Fehler gemeldet."
            }
        }
        val code = root.get("code")?.let(::scalar)?.trim()
        if (!code.isNullOrEmpty() && code != "0" && code != "200") {
            return format(message(root), code) ?: "Druckerfehler $code"
        }
        if (root.get("success")?.let(::scalar)?.trim()?.lowercase() in setOf("false", "0")) {
            return format(message(root), null) ?: "Der Drucker hat den Auftrag abgelehnt."
        }
        return null
    }

    @JvmStatic
    fun failureDetail(body: ByteArray?, fallback: String?): String {
        embeddedError(body)?.let { return it }
        val root = parseObject(body)
        if (root != null) message(root)?.let { return limit(it) }
        val raw = body?.toString(StandardCharsets.UTF_8)?.trim().orEmpty()
        return limit(raw.ifBlank { fallback.orEmpty() })
    }

    private fun parseObject(body: ByteArray?): JsonObject? {
        if (body == null || body.isEmpty()) return null
        return try {
            JsonParser.parseString(body.toString(StandardCharsets.UTF_8)).takeIf { it.isJsonObject }?.asJsonObject
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun message(obj: JsonObject): String? = sequenceOf("message", "msg", "description")
        .mapNotNull { key -> obj.get(key)?.let(::scalar)?.trim()?.takeIf(String::isNotEmpty) }
        .firstOrNull()

    private fun scalar(value: JsonElement): String? = value.takeIf { it.isJsonPrimitive }?.asString

    private fun format(message: String?, code: String?): String? {
        val cleanMessage = message?.trim().orEmpty()
        val cleanCode = code?.trim().orEmpty()
        return when {
            cleanMessage.isNotEmpty() && cleanCode.isNotEmpty() -> limit("$cleanMessage (Code $cleanCode)")
            cleanMessage.isNotEmpty() -> limit(cleanMessage)
            cleanCode.isNotEmpty() -> limit("Druckerfehler $cleanCode")
            else -> null
        }
    }

    private fun limit(detail: String): String = detail.take(MAX_DETAIL_CHARS)
}
