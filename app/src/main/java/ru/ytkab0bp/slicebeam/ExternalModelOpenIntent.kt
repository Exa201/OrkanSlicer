package ru.ytkab0bp.slicebeam

import android.content.Intent
import android.net.Uri

/** Extracts model URIs from the file-open and file-share intents used by Android file apps. */
internal object ExternalModelOpenIntent {
    fun uris(intent: Intent): List<Uri> {
        val result = LinkedHashSet<Uri>()
        intent.data?.let(result::add)

        when (intent.action) {
            Intent.ACTION_SEND -> {
                // Some Android document providers grant and deliver shared files through
                // ClipData without populating EXTRA_STREAM. Read both representations.
                intent.clipData?.let { clip ->
                    for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(result::add)
                }
                (intent.extras?.get(Intent.EXTRA_STREAM) as? Uri)?.let(result::add)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.clipData?.let { clip ->
                    for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(result::add)
                }
                when (val streams = intent.extras?.get(Intent.EXTRA_STREAM)) {
                    is ArrayList<*> -> streams.filterIsInstance<Uri>().forEach(result::add)
                    is List<*> -> streams.filterIsInstance<Uri>().forEach(result::add)
                }
            }
        }
        return result.toList()
    }
}
