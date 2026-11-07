package ru.ytkab0bp.slicebeam

import android.content.Intent
import android.content.ClipData
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepOpenIntentDeviceTest {
    @Test
    fun appCanReceiveStepDocumentsFromContentAndHostlessFileUris() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val requests = listOf(
            Intent(Intent.ACTION_VIEW).setDataAndType(
                Uri.parse("content://documents.example/model.step"), "model/step",
            ),
            Intent(Intent.ACTION_VIEW).setDataAndType(
                Uri.parse("content://documents.example/model.stp"), "application/x-step",
            ),
            // Android file managers may pass a hostless file:// URI and generic MIME type.
            Intent(Intent.ACTION_VIEW).setDataAndType(
                Uri.parse("file:///storage/emulated/0/Download/model.step"), "application/octet-stream",
            ),
            // Some file managers omit the MIME type entirely when sharing a file URL.
            Intent(Intent.ACTION_VIEW).setData(Uri.parse("file:///storage/emulated/0/Download/model.stp")),
            Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://documents.example/document/model.step")),
            Intent(Intent.ACTION_SEND).setType("model/step")
                .putExtra(Intent.EXTRA_STREAM, Uri.parse("content://documents.example/model.step")),
            Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream")
                .putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    arrayListOf(Uri.parse("content://documents.example/a.step")),
                ),
        )

        requests.forEach { request ->
            request.addCategory(Intent.CATEGORY_DEFAULT)
            request.setPackage(context.packageName)
            val resolved = context.packageManager.resolveActivity(request, 0)
            assertNotNull("The app must be offered for $request", resolved)
            assertEquals(MainActivity::class.java.name, resolved?.activityInfo?.name)
        }
    }

    @Test
    fun extractsStepUriFromViewAndSendIntentsIncludingClipData() {
        val step = Uri.parse("content://documents.example/model.step")
        assertEquals(listOf(step), ExternalModelOpenIntent.uris(Intent(Intent.ACTION_VIEW).setData(step)))
        assertEquals(
            listOf(step),
            ExternalModelOpenIntent.uris(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, step)),
        )
        val clipOnlySend = Intent(Intent.ACTION_SEND).apply {
            clipData = ClipData.newRawUri("model", step)
        }
        assertEquals(
            "Some document providers deliver the shared URI through ClipData only",
            listOf(step),
            ExternalModelOpenIntent.uris(clipOnlySend),
        )

        val second = Uri.parse("content://documents.example/second.step")
        val multi = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putExtra(Intent.EXTRA_STREAM, arrayListOf(step, second))
            clipData = ClipData.newRawUri("models", step)
        }
        assertEquals(listOf(step, second), ExternalModelOpenIntent.uris(multi))
    }
}
