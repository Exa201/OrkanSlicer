package ru.ytkab0bp.slicebeam.utils

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Restricts the embedded printer dashboard to the exact configured HTTP(S) origin. */
object PrinterWebOriginPolicy {
    /** Resolves a Moonraker API host to the printer's dashboard endpoint. */
    @JvmStatic
    fun dashboardUrl(configuredHost: String?): String? {
        if (configuredHost == null) return null
        var host = configuredHost.trim()
        if (host.isEmpty()) return null
        if (!host.contains("://")) host = "http://$host"
        try {
            val uri = URI(host)
            if (uri.port == 7125) {
                // 7125 is Moonraker's API; Qidi's Fluidd page is served by the web host on port 80.
                return URI(uri.scheme, null, uri.host, -1, "/", null, null).toString()
            }
            return uri.toString()
        } catch (_: URISyntaxException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
    }

    @JvmStatic
    fun allows(configuredOrigin: String?, targetUrl: String?): Boolean {
        if (configuredOrigin == null || targetUrl == null) return false
        try {
            val origin = URI(configuredOrigin)
            val target = URI(targetUrl)
            val originScheme = normalizedScheme(origin)
            val targetScheme = normalizedScheme(target)
            val originHost = origin.host
            val targetHost = target.host
            return isWebScheme(originScheme)
                    && originScheme == targetScheme
                    && originHost != null && targetHost != null
                    && origin.rawUserInfo == null && target.rawUserInfo == null
                    && originHost.equals(targetHost, ignoreCase = true)
                    && effectivePort(origin, originScheme) == effectivePort(target, targetScheme)
        } catch (_: URISyntaxException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }
    }

    private fun normalizedScheme(uri: URI): String = uri.scheme?.lowercase(Locale.ROOT) ?: ""

    private fun isWebScheme(scheme: String): Boolean = scheme == "http" || scheme == "https"

    private fun effectivePort(uri: URI, scheme: String): Int {
        if (uri.port >= 0) return uri.port
        return if (scheme == "https") 443 else 80
    }
}
