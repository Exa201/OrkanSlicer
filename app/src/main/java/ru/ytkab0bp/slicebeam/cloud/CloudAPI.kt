package ru.ytkab0bp.slicebeam.cloud

import ru.ytkab0bp.sapil.APICallback
import ru.ytkab0bp.sapil.APILibrary
import ru.ytkab0bp.sapil.APIRequestHandle
import ru.ytkab0bp.sapil.APIRunner
import ru.ytkab0bp.sapil.Arg
import ru.ytkab0bp.sapil.Header
import ru.ytkab0bp.slicebeam.BuildConfig
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.io.InputStream
import java.util.ArrayList
import java.util.HashMap

/** Runtime-reflected API contract. Keep the JVM names and annotations stable for SAPIL. */
interface CloudAPI : APIRunner {
    companion object {
        @JvmField
        val INSTANCE: CloudAPI = APILibrary.newRunner(
            CloudAPI::class.java,
            object : APIRunner.RunnerConfig {
                private val headers = HashMap<String, String>()

                override fun getBaseURL(): String = "https://api.beam3d.ru/v1/"

                override fun getDefaultUserAgent(): String =
                    "SliceBeam v${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}"

                override fun getDefaultHeaders(): Map<String, String> {
                    headers.clear()
                    Prefs.getCloudAPIToken()?.let { headers["Authorization"] = "Bearer $it" }
                    return headers
                }
            },
        )
    }

    /** Begins login flow and returns the authorization link. */
    @ru.ytkab0bp.sapil.Method("login/begin")
    fun loginBegin(callback: APICallback<LoginData>): APIRequestHandle

    /** Checks the login state using a session id. */
    @ru.ytkab0bp.sapil.Method("login/check")
    fun loginCheck(@Arg("sessionId") sessionId: String, callback: APICallback<LoginState>)

    /** Cancels the login flow. */
    @ru.ytkab0bp.sapil.Method("login/cancel")
    fun loginCancel(@Arg("sessionId") sessionId: String, callback: APICallback<Boolean>)

    /** Gets information about the current user. */
    @ru.ytkab0bp.sapil.Method("user/getInfo")
    fun userGetInfo(callback: APICallback<UserInfo>)

    /** Gets the account's available features. */
    @ru.ytkab0bp.sapil.Method("user/getFeatures")
    fun userGetFeatures(callback: APICallback<UserFeatures>)

    /** Fetches cloud synchronization state. */
    @ru.ytkab0bp.sapil.Method("sync/getState")
    fun syncGetState(callback: APICallback<SyncState>)

    /** Uploads base64-encoded cloud data. */
    @ru.ytkab0bp.sapil.Method(requestType = ru.ytkab0bp.sapil.RequestType.POST, value = "sync/upload")
    fun syncUpload(@Arg("") data: String, @Header("Content-Type") type: String, callback: APICallback<SyncState>)

    /** Downloads cloud data. */
    @ru.ytkab0bp.sapil.Method("sync/get")
    fun syncGet(callback: APICallback<String>)

    /** Requests a 3D model from a base64-encoded image. */
    @ru.ytkab0bp.sapil.Method(requestType = ru.ytkab0bp.sapil.RequestType.POST, value = "models/generate")
    fun modelsGenerate(@Arg("") image: String, @Header("Content-Type") type: String, callback: APICallback<InputStream>)

    /** Gets the remaining model generation count. */
    @ru.ytkab0bp.sapil.Method("models/getRemainingCount")
    fun modelsGetRemainingCount(callback: APICallback<ModelsRemainingCount>)

    /** Destroys the current cloud token. */
    @ru.ytkab0bp.sapil.Method("logout")
    fun logout(callback: APICallback<Boolean>)

    class LoginData {
        @JvmField var url: String? = null
        @JvmField var sessionId: String? = null
        @JvmField var expiresAt: Long = 0
    }

    class LoginState {
        @JvmField var loggedIn: Boolean = false
        @JvmField var bearer: String? = null
    }

    class UserFeatures {
        @JvmField var earlyAccessLevel: Int = 0
        @JvmField var syncRequiredLevel: Int = 0
        @JvmField var aiGeneratorRequiredLevel: Int = 0
        @JvmField var aiGeneratorModelsPerMonth: Int = 0
        @JvmField var alreadySubscribedInfoUrl: String? = null
        @JvmField var levels: MutableList<SubscriptionLevel> = ArrayList()
    }

    class SubscriptionLevel {
        @JvmField var level: Int = 0
        @JvmField var title: String? = null
        @JvmField var price: String? = null
        @JvmField var subscribeOrUpgradeUrl: String? = null
        @JvmField var manageUrl: String? = null
    }

    class UserInfo {
        @JvmField var id: String? = null
        @JvmField var displayName: String? = null
        @JvmField @androidx.annotation.Nullable var avatarUrl: String? = null
        @JvmField var currentLevel: Int = 0
    }

    class SyncState {
        @JvmField var lastUpdatedDate: Long = 0
        @JvmField var usedSize: Long = 0
        @JvmField var maxSize: Long = 0
    }

    class ModelsRemainingCount {
        @JvmField var used: Int = 0
        @JvmField var max: Int = 0
    }
}
