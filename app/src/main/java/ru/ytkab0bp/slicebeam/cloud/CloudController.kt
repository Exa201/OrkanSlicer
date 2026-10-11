package ru.ytkab0bp.slicebeam.cloud

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import ru.ytkab0bp.sapil.APICallback
import ru.ytkab0bp.sapil.APIRequestHandle
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.events.CloudFeaturesUpdatedEvent
import ru.ytkab0bp.slicebeam.events.CloudLoginStateUpdatedEvent
import ru.ytkab0bp.slicebeam.events.CloudModelsRemainingCountUpdatedEvent
import ru.ytkab0bp.slicebeam.events.CloudSyncFinishedEvent
import ru.ytkab0bp.slicebeam.events.CloudUserInfoUpdatedEvent
import ru.ytkab0bp.slicebeam.events.NeedDismissSnackbarEvent
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.IOUtils
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cloud login and profile-sync coordinator. Public static JVM entry points are kept for Java callers. */
open class CloudController {
    companion object {
    const val USER_INFO_AI_GEN_TAG = "ai_gen_user_info"
    const val CLOUD_SYNC_TAG = "cloud_sync"

    private const val OFFLINE_BOOTSTRAP = true
    private const val TAG = "cloud"
    private const val MIN_SYNC_DELTA = 5 * 60 * 1000L
    private const val MIN_SYNC_FEATURES_DELTA = 12 * 60 * 60 * 1000L

    private var isSyncInProgress = false
    private var userInfo: CloudAPI.UserInfo? = null
    private var userFeatures: CloudAPI.UserFeatures? = null
    private var modelsUsed = 0
    private var modelsMaxGenerations = 0
    private var isLoggingIn = false
    private var beginLoginHandle: APIRequestHandle? = null
    private var loginSessionId: String? = null
    private val gson = Gson()

    private val loginAutoCancel = Runnable {
        loginSessionId = null
        isLoggingIn = false
        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
    }

    private val loginCheck: Runnable = object : Runnable {
        override fun run() {
            CloudAPI.INSTANCE.loginCheck(loginSessionId!!, object : APICallback<CloudAPI.LoginState> {
                override fun onResponse(response: CloudAPI.LoginState) {
                    if (response.loggedIn) {
                        Prefs.setCloudAPIToken(response.bearer)
                        loadUserInfo()
                        ViewUtils.removeCallbacks(loginAutoCancel)
                    } else if (isLoggingIn) {
                        ViewUtils.postOnMainThread(loginCheck, 5000)
                    }
                }

                override fun onException(e: Exception) {
                    Log.e(TAG, "Failed to check login state", e)
                    if (isLoggingIn) ViewUtils.postOnMainThread(loginCheck, 5000)
                }
            })
        }
    }

    @JvmStatic
    fun initCached() {
        if (OFFLINE_BOOTSTRAP) {
            userInfo = null
            userFeatures = null
            modelsUsed = 0
            modelsMaxGenerations = 0
            isLoggingIn = false
            return
        }
        Prefs.getCloudCachedUserFeatures()?.let {
            userFeatures = gson.fromJson(it, CloudAPI.UserFeatures::class.java)
        }
        if (Prefs.getCloudAPIToken() != null) {
            Prefs.getCloudCachedUserInfo()?.let {
                userInfo = gson.fromJson(it, CloudAPI.UserInfo::class.java)
                modelsUsed = Prefs.getCloudCachedUsedModels()
                modelsMaxGenerations = Prefs.getCloudCachedMaxModels()
            }
        }
    }

    @JvmStatic
    fun init() {
        if (OFFLINE_BOOTSTRAP) return
        val now = SliceBeam.TRUE_TIME!!.now().time
        val needSyncInfo = userFeatures == null || now - Prefs.getCloudLastFeaturesSync() > MIN_SYNC_FEATURES_DELTA
        if (needSyncInfo) checkUserFeatures()

        if (Prefs.getCloudAPIToken() != null) {
            if (needSyncInfo || userInfo == null) loadUserInfo()
            if (!needSyncInfo && userInfo != null && isSyncAvailable() && Prefs.isCloudProfileSyncEnabled() &&
                now - Prefs.getCloudLastSync() > MIN_SYNC_DELTA
            ) {
                syncData()
            }
        }
    }

    private fun loadUserInfo() {
        CloudAPI.INSTANCE.userGetInfo(object : APICallback<CloudAPI.UserInfo> {
            override fun onResponse(response: CloudAPI.UserInfo) {
                userInfo = response
                if (response.id!!.equals("null")) {
                    userInfo = null
                    Prefs.setCloudAPIToken(null)
                    Prefs.setCloudCachedUserInfo(null)
                    SliceBeam.EVENT_BUS.fireEvent(CloudUserInfoUpdatedEvent())
                    if (isLoggingIn) {
                        isLoggingIn = false
                        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
                    }
                } else {
                    Prefs.setCloudCachedUserInfo(gson.toJson(userInfo))
                    SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(USER_INFO_AI_GEN_TAG))
                    SliceBeam.EVENT_BUS.fireEvent(CloudUserInfoUpdatedEvent())
                    if (isLoggingIn) {
                        isLoggingIn = false
                        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
                    }
                    if (isSyncAvailable() && Prefs.isCloudProfileSyncEnabled()) syncData()
                    checkGeneratorRemaining()
                }
                Prefs.setCloudLastFeaturesSync(SliceBeam.TRUE_TIME!!.now().time)
            }

            override fun onException(e: Exception) {
                Log.e(TAG, "Failed to get user info", e)
                ViewUtils.postOnMainThread(Runnable { init() }, 15000)
            }
        })
    }

    @JvmStatic
    fun isLoggingIn(): Boolean = isLoggingIn

    private fun beginLogin0() {
        beginLoginHandle = CloudAPI.INSTANCE.loginBegin(object : APICallback<CloudAPI.LoginData> {
            override fun onResponse(response: CloudAPI.LoginData) {
                loginSessionId = response.sessionId
                ViewUtils.postOnMainThread(
                    loginAutoCancel,
                    response.expiresAt * 1000L - SliceBeam.TRUE_TIME!!.now().time,
                )
                ViewUtils.postOnMainThread(loginCheck, 5000)
                ViewUtils.postOnMainThread(Runnable {
                    SliceBeam.INSTANCE!!.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(response.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                })
            }

            override fun onException(e: Exception) {
                ViewUtils.postOnMainThread(Runnable { beginLogin0() }, 15000)
            }
        })
    }

    @JvmStatic
    fun beginLogin() {
        if (OFFLINE_BOOTSTRAP) {
            isLoggingIn = false
            SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
            return
        }
        isLoggingIn = true
        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
        beginLogin0()
    }

    @JvmStatic
    fun cancelLogin() {
        isLoggingIn = false
        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
        if (OFFLINE_BOOTSTRAP) return
        loginSessionId?.let { sessionId -> CloudAPI.INSTANCE.loginCancel(sessionId) {} }
        beginLoginHandle?.let { handle ->
            if (handle.isRunning()) {
                handle.cancel()
                beginLoginHandle = null
            }
        }
        ViewUtils.removeCallbacks(loginCheck)
        ViewUtils.removeCallbacks(loginAutoCancel)
        loginSessionId = null
    }

    @JvmStatic
    fun logout() {
        Prefs.setCloudAPIToken(null)
        userInfo = null
        SliceBeam.EVENT_BUS.fireEvent(CloudLoginStateUpdatedEvent())
        SliceBeam.EVENT_BUS.fireEvent(CloudUserInfoUpdatedEvent())
        if (OFFLINE_BOOTSTRAP) return
        CloudAPI.INSTANCE.logout {}
    }

    @JvmStatic
    fun checkGeneratorRemaining() {
        if (OFFLINE_BOOTSTRAP) return
        CloudAPI.INSTANCE.modelsGetRemainingCount(object : APICallback<CloudAPI.ModelsRemainingCount> {
            override fun onResponse(response: CloudAPI.ModelsRemainingCount) {
                modelsUsed = response.used
                modelsMaxGenerations = response.max
                Prefs.setCloudCachedUsedMaxModels(modelsUsed, modelsMaxGenerations)
                SliceBeam.EVENT_BUS.fireEvent(CloudModelsRemainingCountUpdatedEvent())
            }

            override fun onException(e: Exception) {
                Log.e(TAG, "Failed to check remaining models", e)
                ViewUtils.postOnMainThread(Runnable { checkGeneratorRemaining() }, 15000)
            }
        })
    }

    @JvmStatic
    fun checkUserFeatures() {
        if (OFFLINE_BOOTSTRAP) {
            userFeatures = null
            SliceBeam.EVENT_BUS.fireEvent(CloudFeaturesUpdatedEvent())
            return
        }
        CloudAPI.INSTANCE.userGetFeatures(object : APICallback<CloudAPI.UserFeatures> {
            override fun onResponse(response: CloudAPI.UserFeatures) {
                userFeatures = response
                Prefs.setCloudCachedUserFeatures(gson.toJson(userFeatures))
                if (Prefs.getCloudAPIToken() == null) {
                    Prefs.setCloudLastFeaturesSync(SliceBeam.TRUE_TIME!!.now().time)
                }
                SliceBeam.EVENT_BUS.fireEvent(CloudFeaturesUpdatedEvent())
            }

            override fun onException(e: Exception) {
                Log.e(TAG, "Failed to get user features", e)
                ViewUtils.postOnMainThread(Runnable { checkUserFeatures() }, 15000)
            }
        })
    }

    @JvmStatic
    fun getUserInfo(): CloudAPI.UserInfo? = userInfo

    @JvmStatic
    fun getUserFeatures(): CloudAPI.UserFeatures? = userFeatures

    @JvmStatic
    fun hasAccountFeatures(): Boolean {
        if (OFFLINE_BOOTSTRAP) return false
        return userFeatures?.levels?.isNotEmpty() == true
    }

    @JvmStatic
    fun isSyncAvailable(): Boolean {
        if (OFFLINE_BOOTSTRAP) return false
        val info = userInfo ?: return false
        val features = userFeatures ?: return false
        return Prefs.getCloudAPIToken() != null && info.currentLevel >= features.syncRequiredLevel
    }

    @JvmStatic
    fun needShowAIGenerator(): Boolean {
        if (OFFLINE_BOOTSTRAP) return false
        return userFeatures != null && userFeatures!!.aiGeneratorRequiredLevel >= 0
    }

    @JvmStatic
    fun getGeneratedModels(): Int = modelsUsed

    @JvmStatic
    fun getMaxGeneratedModels(): Int = modelsMaxGenerations

    private fun downloadData(lastModified: Long) {
        CloudAPI.INSTANCE.syncGet(object : APICallback<String> {
            override fun onResponse(response: String) {
                IOUtils.IO_POOL.submit {
                    try {
                        val file = SliceBeam.getConfigFile()
                        val data = Base64.decode(response, 0)
                        FileOutputStream(file).use { it.write(data) }
                        SliceBeam.CONFIG = Slic3rConfigWrapper(file)
                        Prefs.setCloudLocalLastModified(lastModified)
                        Prefs.setCloudLocalLastSentModified(lastModified)
                        Prefs.setCloudRemoteLastModified(lastModified)
                        Prefs.setCloudLastSync(SliceBeam.TRUE_TIME!!.now().time)
                        SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.CloudSyncSuccess))
                        SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to write data", e)
                        isSyncInProgress = false
                        SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                        SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError))
                        SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                    }
                }
            }

            override fun onException(e: Exception) {
                Log.e(TAG, "Failed to download data", e)
                isSyncInProgress = false
                SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError))
                SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
            }
        })
    }

    private fun syncData() {
        if (isSyncInProgress) return
        val modified = Prefs.getCloudLocalLastModified()
        isSyncInProgress = true
        SliceBeam.EVENT_BUS.fireEvent(
            NeedSnackbarEvent(SnackbarsLayout.Type.LOADING, R.string.CloudSyncInProgress).tag(CLOUD_SYNC_TAG),
        )

        CloudAPI.INSTANCE.syncGetState(object : APICallback<CloudAPI.SyncState> {
            override fun onResponse(response: CloudAPI.SyncState) {
                if (SliceBeam.CONFIG == null && response.usedSize != 0L) {
                    downloadData(response.lastUpdatedDate)
                } else if (response.usedSize == 0L) {
                    if (SliceBeam.CONFIG == null) {
                        SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                        SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                        return
                    }
                    uploadData(modified)
                } else if (response.lastUpdatedDate != Prefs.getCloudRemoteLastModified()) {
                    if (Prefs.getCloudLocalLastSentModified() == modified) {
                        downloadData(response.lastUpdatedDate)
                    } else {
                        SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                        SliceBeam.EVENT_BUS.fireEvent(
                            NeedSnackbarEvent(SnackbarsLayout.Type.WARNING, R.string.CloudSyncConflict).button(
                                R.string.CloudSyncConflictResolve,
                            ) { view ->
                                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                BeamAlertDialogBuilder(view.context)
                                    .setTitle(R.string.CloudSyncConflict)
                                    .setMessage(
                                        view.context.getString(
                                            R.string.CloudSyncConflictResolveMessage,
                                            format.format(Date(response.lastUpdatedDate)),
                                            format.format(Date(Prefs.getCloudLocalLastModified())),
                                        ),
                                    )
                                    .setPositiveButton(R.string.CloudSyncConflictChooseRemote) { _, _ ->
                                        downloadData(response.lastUpdatedDate)
                                    }
                                    .setNegativeButton(R.string.CloudSyncConflictChooseLocal) { _, _ ->
                                        uploadData(modified)
                                    }
                                    .show()
                            }.tag(CLOUD_SYNC_TAG),
                        )
                    }
                } else if (Prefs.getCloudLocalLastSentModified() != modified) {
                    uploadData(modified)
                } else {
                    SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                    SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                }
            }

            override fun onException(e: Exception) {
                Log.e(TAG, "Failed to get sync state", e)
                isSyncInProgress = false
                SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError))
            }
        })
    }

    private fun uploadData(modified: Long) {
        IOUtils.IO_POOL.submit {
            try {
                val file = SliceBeam.getConfigFile()
                val data = ByteArrayOutputStream().use { output ->
                    FileInputStream(file).use { input -> input.copyTo(output) }
                    output.toByteArray()
                }
                CloudAPI.INSTANCE.syncUpload(
                    Base64.encodeToString(data, Base64.NO_WRAP),
                    "application/ini",
                    object : APICallback<CloudAPI.SyncState> {
                        override fun onResponse(response: CloudAPI.SyncState) {
                            isSyncInProgress = false
                            if (Prefs.getCloudLocalLastModified() != modified) {
                                syncData()
                                return
                            }
                            Prefs.setCloudRemoteLastModified(response.lastUpdatedDate)
                            Prefs.setCloudLocalLastSentModified(modified)
                            Prefs.setCloudLastSync(SliceBeam.TRUE_TIME!!.now().time)
                            SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.CloudSyncSuccess))
                            SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                        }

                        override fun onException(e: Exception) {
                            Log.e(TAG, "Failed to upload sync data", e)
                            isSyncInProgress = false
                            SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError))
                            SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
                        }
                    },
                )
            } catch (e: IOException) {
                Log.e(TAG, "Failed to read sync data", e)
                isSyncInProgress = false
                SliceBeam.EVENT_BUS.fireEvent(NeedDismissSnackbarEvent(CLOUD_SYNC_TAG))
                SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(SnackbarsLayout.Type.ERROR, R.string.CloudSyncError))
                SliceBeam.EVENT_BUS.fireEvent(CloudSyncFinishedEvent())
            }
        }
    }

    @JvmStatic
    fun notifyDataChanged() {
        // Setup may save before the non-critical TrueTime task has completed.
        val now = SliceBeam.TRUE_TIME?.now()?.time ?: System.currentTimeMillis()
        Prefs.setCloudLocalLastModified(now)
        if (SliceBeam.TRUE_TIME == null) return
        if (!isSyncAvailable() || !Prefs.isCloudProfileSyncEnabled()) return
        if (now - Prefs.getCloudLastSync() > MIN_SYNC_DELTA) syncData()
    }
    }
}
