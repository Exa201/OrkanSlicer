package ru.ytkab0bp.sapil

import android.util.Log
import kotlin.jvm.JvmDefaultWithCompatibility

@JvmDefaultWithCompatibility
fun interface APICallback<T> {
    fun onResponse(response: T)

    fun onException(e: Exception) {
        Log.e("sapil", "Failed to execute request", e)
    }
}
