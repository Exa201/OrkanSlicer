package ru.ytkab0bp.slicebeam.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** Checks the official GitHub release and delegates APK transfers to Android's download service. */
object GitHubUpdater {
    private const val LATEST_RELEASE = "https://api.github.com/repos/Exa201/OrkanSlicer/releases/latest"
    private const val RELEASE_ASSET_PREFIX = "https://github.com/Exa201/OrkanSlicer/releases/download/"
    private const val MAX_APK_BYTES = 300L * 1024 * 1024
    private const val PREFS = "github_updater"
    private const val KEY_LAST_CHECKED = "last_checked_at"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_DOWNLOAD_VERSION = "download_version"
    private const val STARTUP_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
    private val main by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Handler(Looper.getMainLooper()) }
    private val checkInProgress = AtomicBoolean(false)

    /** Checks at launch, throttled to avoid exhausting GitHub's unauthenticated API limit. */
    fun checkAtAppStart(context: Context) {
        val appContext = context.applicationContext
        try {
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            // Inspect an older transfer before applying the API throttle. This lets the next
            // launch retry a failed download and removes the downloaded APK after it was installed.
            val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            if (hasUsableExistingDownload(appContext, manager)) return
            if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_CHECKED, 0L) < STARTUP_CHECK_INTERVAL_MS) return
            checkForUpdate(appContext, status = null)
        } catch (_: Exception) {
            // Update availability must never prevent the slicer workspace from opening.
        }
    }

    /** Manual check from the About page, which always bypasses the startup throttle. */
    fun check(activity: Activity, status: (String) -> Unit) {
        checkForUpdate(activity.applicationContext) { message ->
            if (!activity.isFinishing && !activity.isDestroyed) status(message)
        }
    }

    private fun checkForUpdate(context: Context, status: ((String) -> Unit)?) {
        if (!checkInProgress.compareAndSet(false, true)) {
            post(status, "Die Update-Prüfung läuft bereits.")
            return
        }

        Thread({
            try {
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                if (hasUsableExistingDownload(context, manager)) {
                    post(status, "Ein Update wird bereits im Hintergrund geladen oder ist heruntergeladen.")
                    return@Thread
                }

                val connection = open(LATEST_RELEASE)
                val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                connection.disconnect()
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()

                val version = release.optString("tag_name").removePrefix("v")
                if (release.optBoolean("draft") || release.optBoolean("prerelease") ||
                    version.isBlank() || compareVersions(version, BuildConfig.VERSION_NAME) <= 0
                ) {
                    post(status, "Du verwendest die aktuellste veröffentlichte Version.")
                    return@Thread
                }

                val assets = release.optJSONArray("assets")
                var apkUrl: String? = null
                var apkSize = 0L
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.getJSONObject(index)
                        val name = asset.optString("name")
                        val url = asset.optString("browser_download_url")
                        val size = asset.optLong("size", -1L)
                        if (name.endsWith(".apk", ignoreCase = true) &&
                            url.startsWith(RELEASE_ASSET_PREFIX) && size in 1..MAX_APK_BYTES
                        ) {
                            apkUrl = url
                            apkSize = size
                            break
                        }
                    }
                }
                val downloadUrl = apkUrl
                if (downloadUrl == null) {
                    post(status, "Version $version gefunden, aber das Release enthält keine gültige APK.")
                    return@Thread
                }

                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle("OrkanSlicer $version")
                    .setDescription("Update wird im Hintergrund geladen (${formatMegabytes(apkSize)} MB).")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        "OrkanSlicer-$version.apk",
                    )
                val id = manager.enqueue(request)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_DOWNLOAD_ID, id)
                    .putString(KEY_DOWNLOAD_VERSION, version)
                    .apply()
                post(status, "Version $version wird im Hintergrund geladen. Android zeigt den Download an.")
            } catch (e: Exception) {
                post(status, "Update-Prüfung fehlgeschlagen: ${e.localizedMessage ?: "Verbindung prüfen"}")
            } finally {
                checkInProgress.set(false)
            }
        }, "orkanslicer-update-check").start()
    }

    private fun hasUsableExistingDownload(context: Context, manager: DownloadManager): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0L) return false
        val downloadedVersion = prefs.getString(KEY_DOWNLOAD_VERSION, null)
        if (downloadedVersion != null && compareVersions(downloadedVersion, BuildConfig.VERSION_NAME) <= 0) {
            manager.remove(id)
            prefs.edit().remove(KEY_DOWNLOAD_ID).remove(KEY_DOWNLOAD_VERSION).apply()
            return false
        }
        val query = DownloadManager.Query().setFilterById(id)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) {
                prefs.edit().remove(KEY_DOWNLOAD_ID).remove(KEY_DOWNLOAD_VERSION).apply()
                return false
            }
            return when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED,
                DownloadManager.STATUS_SUCCESSFUL -> true
                else -> {
                    prefs.edit().remove(KEY_DOWNLOAD_ID).remove(KEY_DOWNLOAD_VERSION).apply()
                    false
                }
            }
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "OrkanSlicer-Android")
            setRequestProperty("Accept", "application/vnd.github+json")
            instanceFollowRedirects = true
            connect()
            if (responseCode !in 200..299) error("HTTP $responseCode")
        }

    internal fun compareVersions(a: String, b: String): Int {
        val left = parseVersion(a) ?: return -1
        val right = parseVersion(b) ?: return 1
        for (index in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parseVersion(value: String): List<Int>? {
        val normalized = value.removePrefix("v").substringBefore('-').substringBefore('+')
        if (normalized.isBlank() || !normalized.matches(Regex("\\d+(\\.\\d+)*"))) return null
        return normalized.split('.').map { it.toIntOrNull() ?: return null }
    }

    private fun formatMegabytes(bytes: Long): String = "%.1f".format(bytes / (1024.0 * 1024.0))

    private fun post(status: ((String) -> Unit)?, message: String) {
        if (status != null) main.post { status(message) }
    }
}
