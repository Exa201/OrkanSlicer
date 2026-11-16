package ru.ytkab0bp.slicebeam.utils

import android.net.Uri
import org.json.JSONArray

data class RecentModelDocument(val uri: String, val displayName: String)

/** Small, private list of SAF documents whose read permission survives app restarts. */
object RecentModelDocuments {
    private const val PREF_KEY = "recent_model_documents_v1"
    private const val LIMIT = 10

    @Synchronized
    fun list(): List<RecentModelDocument> {
        val prefs = Prefs.getPrefs() ?: return emptyList()
        val raw = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    val uri = item.optString("uri").takeIf(String::isNotBlank) ?: continue
                    val name = item.optString("name").takeIf(String::isNotBlank) ?: Uri.parse(uri).lastPathSegment.orEmpty()
                    add(RecentModelDocument(uri, name))
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(uri: Uri, displayName: String) {
        if (uri.scheme != "content" || !isModelDocument(displayName)) return
        val normalized = uri.toString()
        val updated = (list().filterNot { it.uri == normalized } + RecentModelDocument(normalized, displayName))
            .takeLast(LIMIT)
        val json = JSONArray().apply {
            updated.forEach { item ->
                put(org.json.JSONObject().put("uri", item.uri).put("name", item.displayName))
            }
        }
        Prefs.getPrefs()?.edit()?.putString(PREF_KEY, json.toString())?.apply()
    }

    private fun isModelDocument(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in setOf("3mf", "stl", "obj", "amf", "svg", "step", "stp", "oltp", "drc")
    }
}
