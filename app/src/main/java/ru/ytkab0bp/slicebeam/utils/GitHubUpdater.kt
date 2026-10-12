package ru.ytkab0bp.slicebeam.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONArray
import ru.ytkab0bp.slicebeam.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Installs only APK assets published in this project's GitHub releases. */
object GitHubUpdater {
    private const val RELEASES = "https://api.github.com/repos/Exa201/OrkanSlicer/releases?per_page=15"
    private const val MAX_APK_BYTES = 300L * 1024 * 1024
    private val main = Handler(Looper.getMainLooper())

    fun check(activity: Activity, status: (String) -> Unit) {
        Thread {
            try {
                val connection = open(RELEASES)
                val releases = connection.inputStream.bufferedReader().use { JSONArray(it.readText()) }
                connection.disconnect()
                var chosen: Pair<String, String>? = null
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    if (release.optBoolean("draft")) continue
                    val version = release.optString("tag_name").removePrefix("v")
                    if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) continue
                    val assets = release.optJSONArray("assets") ?: continue
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val name = asset.optString("name")
                        val url = asset.optString("browser_download_url")
                        if (name.endsWith(".apk", true) && url.startsWith("https://github.com/Exa201/OrkanSlicer/releases/download/")) {
                            if (chosen == null || compareVersions(version, chosen.first) > 0) chosen = version to url
                            break
                        }
                    }
                }
                val update = chosen
                if (update == null) post(status, "Du verwendest die aktuellste veröffentlichte Version.")
                else {
                    post(status, "Version ${update.first} gefunden. Lade APK herunter …")
                    download(activity, update.first, update.second, status)
                }
            } catch (e: Exception) {
                post(status, "Update-Prüfung fehlgeschlagen: ${e.localizedMessage ?: "Verbindung prüfen"}")
            }
        }.start()
    }

    private fun download(activity: Activity, version: String, url: String, status: (String) -> Unit) {
        val file = File(activity.cacheDir, "OrkanSlicer_update.apk")
        val partial = File(activity.cacheDir, "OrkanSlicer_update.apk.part")
        try {
            val connection = open(url)
            val length = connection.contentLengthLong
            if (length > MAX_APK_BYTES) error("APK ist zu groß")
            var received = 0L
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastPercent = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > MAX_APK_BYTES) error("APK ist zu groß")
                        output.write(buffer, 0, count)
                        if (length > 0) {
                            val percent = (received * 100 / length).toInt()
                            if (percent / 10 != lastPercent / 10) {
                                lastPercent = percent
                                post(status, "Version $version wird geladen: $percent %")
                            }
                        }
                    }
                }
            }
            connection.disconnect()
            if (received == 0L || (length > 0 && received != length)) error("Download unvollständig")
            if (file.exists()) file.delete()
            if (!partial.renameTo(file)) error("APK konnte nicht gespeichert werden")
            main.post {
                try {
                    val uri = FileProvider.getUriForFile(activity, "${BuildConfig.APPLICATION_ID}.slicermore.files", file)
                    activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                    status("Version $version heruntergeladen. Installation in Android bestätigen.")
                } catch (e: Exception) {
                    status("APK geladen, Installationsdialog konnte nicht geöffnet werden: ${e.localizedMessage}")
                }
            }
        } finally {
            partial.delete()
        }
    }

    private fun open(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "OrkanSlicer-Android")
            instanceFollowRedirects = true
            connect()
            if (responseCode !in 200..299) error("HTTP $responseCode")
        }
    }

    internal fun compareVersions(a: String, b: String): Int {
        val left = a.split('.').map { it.toIntOrNull() ?: return -1 }
        val right = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = (left.getOrElse(i) { 0 }).compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun post(status: (String) -> Unit, message: String) = main.post { status(message) }
}
