package ru.ytkab0bp.slicebeam

import android.util.Log
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.ytkab0bp.slicebeam.cloud.CloudController
import ru.ytkab0bp.slicebeam.events.BeamServerDataUpdatedEvent
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

open class BeamServerData(obj: JSONObject) {
    @JvmField
    var boostySubscribers: MutableList<String> = ArrayList()

    init {
        val subscribers: JSONArray? = obj.optJSONArray("boosty_subscribers")
        if (subscribers != null) {
            for (index in 0 until subscribers.length()) {
                boostySubscribers.add(subscribers.optString(index))
            }
        }
    }

    companion object {
        private const val TAG = "BeamServerData"
        private const val OFFLINE_BOOTSTRAP = true
        private const val DATA_URL = "https://beam3d.ru/slicebeam.php?act=get_data"
        private const val RUSSIA_CHECK_URL = "https://beam3d.ru/check_russia.txt"
        private val client = AsyncHttpClient().apply {
            setUserAgent(String.format(Locale.ROOT, "SliceBeam/%s-%d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            setEnableRedirects(true)
            setLoggingEnabled(false)
        }

        @JvmStatic
        fun isBoostyAvailable(): Boolean {
            if (OFFLINE_BOOTSTRAP) return false
            return !BuildConfig.IS_GOOGLE_PLAY || Prefs.isRussianIP()
        }

        @JvmStatic
        fun isCloudAvailable(): Boolean {
            if (OFFLINE_BOOTSTRAP) return false
            return isBoostyAvailable() && CloudController.hasAccountFeatures()
        }

        @JvmStatic
        fun load() {
            if (OFFLINE_BOOTSTRAP) {
                SliceBeam.SERVER_DATA = BeamServerData(JSONObject())
                SliceBeam.EVENT_BUS.fireEvent(BeamServerDataUpdatedEvent())
                return
            }
            client.get(DATA_URL, object : AsyncHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<Header>?, responseBody: ByteArray?) {
                    val data = String(responseBody!!, StandardCharsets.UTF_8)
                    Prefs.setBeamServerData(data)
                    Prefs.setLastCheckedInfo()

                    SliceBeam.SERVER_DATA = try {
                        BeamServerData(JSONObject(data))
                    } catch (error: JSONException) {
                        throw RuntimeException(error)
                    }

                    // Disable Boosty only for Google Play builds on non-Russian IPs.
                    if (BuildConfig.IS_GOOGLE_PLAY) {
                        client.get(RUSSIA_CHECK_URL, object : AsyncHttpResponseHandler() {
                            override fun onSuccess(
                                statusCode: Int,
                                headers: Array<Header>?,
                                responseBody: ByteArray?,
                            ) {
                                setIsRussia(String(responseBody!!).equals("true"))
                            }

                            override fun onFailure(
                                statusCode: Int,
                                headers: Array<Header>?,
                                responseBody: ByteArray?,
                                error: Throwable?,
                            ) {
                                if (statusCode == 403) setIsRussia(false)
                            }

                            private fun setIsRussia(isRussia: Boolean) {
                                Prefs.setRussianIP(isRussia)
                                SliceBeam.EVENT_BUS.fireEvent(BeamServerDataUpdatedEvent())
                            }
                        })
                    } else {
                        SliceBeam.EVENT_BUS.fireEvent(BeamServerDataUpdatedEvent())
                    }
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>?,
                    responseBody: ByteArray?,
                    error: Throwable?,
                ) {
                    Log.e(TAG, "Failed to update server data", error)
                }
            })
        }
    }
}
