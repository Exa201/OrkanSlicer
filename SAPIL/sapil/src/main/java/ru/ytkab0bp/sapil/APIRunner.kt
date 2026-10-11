package ru.ytkab0bp.sapil

import ru.ytkab0bp.sapil.util.Pair

interface APIRunner {
    @JvmDefaultWithCompatibility
    interface RunnerConfig {
        fun getBaseURL(): String? = null
        fun getPathPrefix(): String = ""
        fun getNamingTransformPolicy(): Pair<String, String>? = null
        fun getBooleansAsInt(): Boolean = true
        fun getDefaultUserAgent(): String = DEFAULT_USER_AGENT
        fun getDefaultHeaders(): Map<String, String>? = null
    }

    companion object {
        const val DEFAULT_USER_AGENT: String = "SAPIL v${BuildConfig.VERSION}/${BuildConfig.VERSION_CODE}"
    }
}
