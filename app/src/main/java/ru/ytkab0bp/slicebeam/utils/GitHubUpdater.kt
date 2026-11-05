package ru.ytkab0bp.slicebeam.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

internal data class NightlyReleaseCandidate(
    val tagName: String,
    val prerelease: Boolean,
    val draft: Boolean,
    val assets: JSONArray? = null,
)

enum class UpdateDownloadStatus { NONE, PENDING, RUNNING, PAUSED, COMPLETE, FAILED }

data class UpdateDownloadState(
    val status: UpdateDownloadStatus = UpdateDownloadStatus.NONE,
    val version: String? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val reason: Int = 0,
) {
    val progressPercent: Int?
        get() = if (totalBytes > 0L) {
            ((downloadedBytes.coerceAtLeast(0L) * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else null
}

/** Checks the official GitHub release and delegates APK transfers to Android's download service. */
object GitHubUpdater {
    private const val RELEASES = "https://api.github.com/repos/Exa201/OrkanSlicer-Nightly-2.5/releases?per_page=30"
    private const val RELEASE_ASSET_PREFIX = "https://github.com/Exa201/OrkanSlicer-Nightly-2.5/releases/download/"
    private const val MAX_APK_BYTES = 300L * 1024 * 1024
    private const val PREFS = "github_updater"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_DOWNLOAD_VERSION = "download_version"
    private val main by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Handler(Looper.getMainLooper()) }
    private val checkInProgress = AtomicBoolean(false)
    private val startupCheckTriggered = AtomicBoolean(false)

    /** Checks once per app process at launch, unless a transfer is already available. */
    fun checkAtAppStart(context: Context) {
        if (!startupCheckTriggered.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        try {
            // Keep a running or completed transfer and remove it after it was installed.
            val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            if (hasUsableExistingDownload(appContext, manager)) return
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

    /** Snapshot that lets the About screen recover a transfer after its system notification is gone. */
    fun downloadState(context: Context): UpdateDownloadState {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        val version = prefs.getString(KEY_DOWNLOAD_VERSION, null)
        if (id < 0L) return UpdateDownloadState()
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return UpdateDownloadState(version = version, status = UpdateDownloadStatus.FAILED)
        return try {
            manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (!cursor.moveToFirst()) {
                    UpdateDownloadState(version = version, status = UpdateDownloadStatus.FAILED)
                } else {
                    val status = when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_PENDING -> UpdateDownloadStatus.PENDING
                        DownloadManager.STATUS_RUNNING -> UpdateDownloadStatus.RUNNING
                        DownloadManager.STATUS_PAUSED -> UpdateDownloadStatus.PAUSED
                        DownloadManager.STATUS_SUCCESSFUL -> UpdateDownloadStatus.COMPLETE
                        else -> UpdateDownloadStatus.FAILED
                    }
                    UpdateDownloadState(
                        status = status,
                        version = version,
                        downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                        reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                    )
                }
            }
        } catch (_: Exception) {
            UpdateDownloadState(version = version, status = UpdateDownloadStatus.FAILED)
        }
    }

    /** Opens the package installer only after an explicit user action. */
    fun installDownloaded(activity: Activity, status: (String) -> Unit) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0L || downloadState(activity).status != UpdateDownloadStatus.COMPLETE) {
            status("Das Update ist noch nicht vollständig heruntergeladen.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            try {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")),
                )
                status("Erlaube OrkanSlicer Nightly die Installation unbekannter Apps. Danach hier auf „Installieren“ tippen.")
            } catch (error: Exception) {
                status("Installationsberechtigung konnte nicht geöffnet werden: ${error.localizedMessage ?: "Einstellungen prüfen"}")
            }
            return
        }
        try {
            val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = manager.getUriForDownloadedFile(id)
                ?: error("Der Downloadmanager findet die fertige APK nicht mehr")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
            status("Installationsdialog für Version ${prefs.getString(KEY_DOWNLOAD_VERSION, "").orEmpty()} geöffnet.")
        } catch (error: Exception) {
            status("Installation konnte nicht gestartet werden: ${error.localizedMessage ?: "APK erneut laden"}")
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

                val connection = open(RELEASES)
                val releases = connection.inputStream.bufferedReader().use { JSONArray(it.readText()) }
                connection.disconnect()
                val release = selectNightlyRelease((0 until releases.length()).mapNotNull { index ->
                    val json = releases.optJSONObject(index) ?: return@mapNotNull null
                    NightlyReleaseCandidate(
                        json.optString("tag_name"),
                        json.optBoolean("prerelease"),
                        json.optBoolean("draft"),
                        json.optJSONArray("assets"),
                    )
                }, BuildConfig.VERSION_NAME)
                if (release == null) {
                    post(status, "Du verwendest die aktuellste veröffentlichte Version.")
                    return@Thread
                }
                val version = release.tagName.removePrefix("v")

                val assets = release.assets
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
                    .setTitle("OrkanSlicer Nightly 2.5 $version")
                    .setDescription("Update wird im Hintergrund geladen (${formatMegabytes(apkSize)} MB).")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        "OrkanSlicerNightly25-$version.apk",
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

    internal fun selectNightlyRelease(
        releases: List<NightlyReleaseCandidate>,
        installedVersion: String,
    ): NightlyReleaseCandidate? {
        var selected: NightlyReleaseCandidate? = null
        var selectedVersion: String? = null
        for (candidate in releases) {
            if (candidate.draft || !candidate.prerelease) continue
            val version = candidate.tagName.removePrefix("v")
            if (version.isBlank() || compareVersions(version, installedVersion) <= 0) continue
            if (selectedVersion == null || compareVersions(version, selectedVersion) > 0) {
                selected = candidate
                selectedVersion = version
            }
        }
        return selected
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
