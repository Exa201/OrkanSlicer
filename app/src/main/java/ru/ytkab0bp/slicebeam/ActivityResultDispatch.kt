package ru.ytkab0bp.slicebeam

import android.app.Activity
import android.content.Intent

/** Sends workspace picker/export requests through MainActivity's lifecycle-aware launchers. */
fun Activity.launchAppActivityResult(intent: Intent, requestCode: Int) {
    val host = this as? MainActivity
        ?: throw IllegalStateException("Workspace activity results require MainActivity")
    host.launchRegisteredActivityResult(intent, requestCode)
}
