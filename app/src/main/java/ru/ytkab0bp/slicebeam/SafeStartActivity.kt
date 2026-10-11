package ru.ytkab0bp.slicebeam

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.ytkab0bp.slicebeam.ui.SafeStartScreen
import ru.ytkab0bp.slicebeam.utils.Prefs

/** A small, theme-independent recovery screen shown after an uncaught app crash. */
class SafeStartActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkSystemTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val systemBarColor = if (darkSystemTheme) Color.rgb(17, 19, 24) else Color.WHITE
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkSystemTheme
            isAppearanceLightNavigationBars = !darkSystemTheme
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val crashReport = getString(
            R.string.AppCrashedDesc,
            Build.VERSION.RELEASE,
            "${Build.BRAND} ${Build.MODEL}",
            Prefs.getPrefs()?.getString("crash", "").orEmpty(),
        )

        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SafeStartScreen(
                        crashReport = crashReport,
                        onShare = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, crashReport)
                                type = "text/plain"
                            }
                            startActivity(Intent.createChooser(sendIntent, null))
                        },
                        onRestart = {
                            Prefs.getPrefs()?.edit()?.remove("crash")?.apply()
                            startActivity(Intent(this@SafeStartActivity, MainActivity::class.java))
                            finish()
                        },
                    )
                }
            },
        )
    }
}
