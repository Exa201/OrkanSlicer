package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.launchAppActivityResult
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Bundled Kromacut editor shown inside the converter panel. */
class KromacutEditorMenu : WorkspaceComposeToolRoute() {
    private var webView: WebView? = null
    private var outputStream: FileOutputStream? = null
    private var outputFile: File? = null
    private var activeError by mutableStateOf<String?>(null)

    companion object {
        const val REQUEST_CODE_FILE = 15
        private const val EDITOR_URL = "https://appassets.androidplatform.net/assets/kromacut-editor/index.html"

        private var fileCallback: ValueCallback<Array<Uri>>? = null
        @JvmStatic
        fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode != REQUEST_CODE_FILE) return false
            val callback = fileCallback
            fileCallback = null
            if (callback != null) {
                val results = if (resultCode == Activity.RESULT_OK && data != null) {
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                } else {
                    null
                }
                callback.onReceiveValue(results)
            }
            return true
        }
    }

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val context = LocalContext.current
        Column(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                TextButton(
                    onClick = { dismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.KromacutEditorClose))
                }
            }
            AndroidView(
                factory = { createEditor(context).also { it.loadUrl(EDITOR_URL) } },
                modifier = Modifier.weight(1f),
            )
            activeError?.let { detail ->
                KromacutErrorDialog(detail) { activeError = null }
            }
        }
    }

    private fun createEditor(ctx: Context): WebView {
        return WebView(ctx).apply {
            setBackgroundColor(0xFF101115.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            addJavascriptInterface(DownloadBridge(), "OrcaMobile")
            webView = this

            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()
            webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != "appassets.androidplatform.net"
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams,
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = callback
                    return try {
                        val chooser = params.createIntent()
                        (ctx as Activity).launchAppActivityResult(chooser, REQUEST_CODE_FILE)
                        true
                    } catch (error: Exception) {
                        fileCallback = null
                        callback.onReceiveValue(null)
                        activeError = ctx.getString(R.string.ConverterFolderPickerUnavailable)
                        false
                    }
                }
            }
        }
    }

    override fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
        if (portrait) into.height - into.paddingTop - into.paddingBottom
        else into.width - into.paddingLeft - into.paddingRight

    override fun onDestroy() {
        activeError = null
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        try {
            outputStream?.close()
        } catch (_: Exception) {
        }
        outputStream = null
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }

    private fun beginDownload(filename: String?) {
        try {
            outputStream?.close()
            val safeName = File(filename ?: "kromacut-output").name
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val cache = fragment?.getContext()?.cacheDir
                ?: throw IllegalStateException("Der Zwischenspeicher ist nicht verfügbar.")
            outputFile = File(
                cache,
                "kromacut-${System.currentTimeMillis()}-$safeName",
            )
            outputStream = FileOutputStream(outputFile!!)
        } catch (error: Exception) {
            failDownload(error.message)
        }
    }

    private fun writeData(base64: String) {
        try {
            val stream = outputStream ?: throw IllegalStateException("Kein Export geöffnet.")
            stream.write(Base64.decode(base64, Base64.DEFAULT))
        } catch (error: Exception) {
            failDownload(error.message)
        }
    }

    private fun finishDownload() {
        try {
            val stream = outputStream ?: throw IllegalStateException("Der Export ist leer.")
            val file = outputFile ?: throw IllegalStateException("Der Export ist leer.")
            stream.close()
            outputStream = null
            outputFile = null
            val extension = file.name.lowercase(Locale.ROOT).substringAfterLast('.', "bin")
            ViewUtils.postOnMainThread {
                dismiss()
                val activity = fragment?.getContext() as? Activity
                val bed = fragment
                if (activity != null && bed != null) {
                    ImageToStlConverter.saveGeneratedFile(activity, file, extension, bed)
                } else {
                    file.delete()
                }
            }
        } catch (error: Exception) {
            failDownload(error.message)
        }
    }

    private fun failDownload(message: String?) {
        try {
            outputStream?.close()
        } catch (_: Exception) {
        }
        outputStream = null
        outputFile?.delete()
        outputFile = null
        val detail = message ?: "Kromacut konnte die Datei nicht speichern."
        ViewUtils.postOnMainThread {
            if (fragment?.getView() == null) return@postOnMainThread
            activeError = detail
        }
    }

    private inner class DownloadBridge {
        @JavascriptInterface
        fun beginDownload(filename: String?) = this@KromacutEditorMenu.beginDownload(filename)

        @JavascriptInterface
        fun writeData(chunk: String) = this@KromacutEditorMenu.writeData(chunk)

        @JavascriptInterface
        fun finishDownload() = this@KromacutEditorMenu.finishDownload()

        @JavascriptInterface
        fun failDownload(message: String?) = this@KromacutEditorMenu.failDownload(message)
    }
}

@Composable
internal fun KromacutErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ConverterFailed)) },
        text = {
            Text(
                text = message,
                modifier = Modifier
                    .testTag("kromacut-error-message")
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.5f)
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
