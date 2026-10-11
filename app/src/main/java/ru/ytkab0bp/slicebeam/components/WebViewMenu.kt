package ru.ytkab0bp.slicebeam.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ru.ytkab0bp.slicebeam.BuildConfig
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.events.NeedDismissCalibrationsMenu
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.view.DividerView
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

open class WebViewMenu : UnfoldMenu {
    private val uri: Uri
    private var javascript: String? = null
    private var gcodeFragment: BedFragment? = null

    private var fileStream: FileOutputStream? = null
    private var cacheFile: File? = null

    constructor(uri: Uri) {
        this.uri = uri
    }

    constructor(uri: Uri, javascript: String?) : this(uri) {
        this.javascript = javascript
    }

    fun setFragment(fragment: BedFragment): WebViewMenu {
        gcodeFragment = fragment
        return this
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(ctx: Context, portrait: Boolean): View {
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val toolbar = LinearLayout(ctx).apply {
            setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ViewUtils.createRipple(
                ThemesRepo.getColor(android.R.attr.colorControlHighlight),
                0f,
            )
            setOnClickListener { dismiss() }
        }

        val icon = ImageView(ctx).apply {
            setImageResource(R.drawable.arrow_left_outline_28)
            setColorFilter(ThemesRepo.getColor(android.R.attr.textColorSecondary))
        }
        toolbar.addView(icon, LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)))

        val title = TextView(ctx).apply {
            setText(R.string.MenuOrientationPositionBack)
            typeface = ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
            setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary))
        }
        toolbar.addView(
            title,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ViewUtils.dp(12)
            },
        )
        layout.addView(
            toolbar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)),
        )
        layout.addView(
            DividerView(ctx),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(1f)),
        )

        val webView = object : WebView(ctx) {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                if (fileStream != null) return true
                return super.dispatchTouchEvent(event)
            }
        }
        webView.addJavascriptInterface(Bridge(), "SliceBeam")

        webView.settings.apply {
            userAgentString = String.format(
                Locale.ROOT,
                "SliceBeam/%s-%d",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            )
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(false)
        }

        webView.loadUrl(uri.toString())
        webView.alpha = 0f
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val script = javascript
                if (script != null) {
                    webView.evaluateJavascript(script) {
                        ViewUtils.postOnMainThread { webView.animate().alpha(1f).start() }
                    }
                } else {
                    webView.animate().alpha(1f).start()
                }
            }
        }
        layout.addView(
            webView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        return layout
    }

    override fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
        if (portrait) into.height - into.paddingTop - into.paddingBottom else into.width

    private inner class Bridge {
        @JavascriptInterface
        fun beginDownload(filename: String) {
            val target = File(SliceBeam.getModelCacheDir(), filename)
            cacheFile = target
            try {
                fileStream = FileOutputStream(target)
            } catch (error: Exception) {
                Log.e("WebViewMenu", "Failed to begin download", error)
            }
        }

        @JavascriptInterface
        fun writeData(data: String) {
            try {
                fileStream!!.write(Base64.decode(data, 0))
            } catch (error: Exception) {
                Log.e("WebViewMenu", "Failed to write to stream", error)
            }
        }

        @JavascriptInterface
        fun finishDownload() {
            try {
                fileStream!!.close()
                ViewUtils.postOnMainThread {
                    dismiss(true)
                    SliceBeam.EVENT_BUS.fireEvent(NeedDismissCalibrationsMenu())
                    ViewUtils.postOnMainThread({
                        gcodeFragment!!.loadGCode(cacheFile!!)
                    }, 200)
                }
            } catch (error: Exception) {
                Log.e("WebViewMenu", "Failed to finish file", error)
            }
        }
    }
}
