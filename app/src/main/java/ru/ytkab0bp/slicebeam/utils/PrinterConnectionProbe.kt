package ru.ytkab0bp.slicebeam.utils

import java.net.HttpURLConnection
import java.net.URL

/** A short reachability check for the currently configured printer web interface. */
internal object PrinterConnectionProbe {
    fun isReachable(host: String, timeoutMillis: Int = 1_500): Boolean {
        val connection = try {
            (URL(host).openConnection() as? HttpURLConnection) ?: return false
        } catch (_: Exception) {
            return false
        }
        return try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.connect()
            // Even an authorization error, unsupported HEAD request, or server-side
            // error proves the configured printer host is reachable.
            connection.responseCode in 100..599
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
