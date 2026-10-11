package ru.ytkab0bp.slicebeam.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.Prefs

object ThemesRepo {
    private var resolvedSystemMode: Boolean? = null

    @JvmStatic
    fun getCurrent(): BeamTheme {
        if (Prefs.getThemeMode() == Prefs.ThemeMode.SYSTEM) {
            if (resolvedSystemMode == null) {
                resolvedSystemMode = (SliceBeam.requireInstance().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            return if (resolvedSystemMode == true) BeamTheme.DARK else BeamTheme.LIGHT
        }
        return if (Prefs.getThemeMode() == Prefs.ThemeMode.LIGHT) BeamTheme.LIGHT else BeamTheme.DARK
    }

    @JvmStatic
    fun resetSystemResolvedTheme() {
        resolvedSystemMode = null
    }

    @JvmStatic
    fun getColor(resourceId: Int): Int = getCurrent().colors.get(resourceId)

    @JvmStatic
    fun invalidate(activity: Activity) {
        if (activity is MainActivity) {
            activity.onApplyTheme()
        } else {
            invalidateView(activity.window.decorView)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @JvmStatic
    fun invalidateView(view: View?) {
        if (view == null) return
        if (view is IThemeView) {
            view.onApplyTheme()
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                invalidateView(view.getChildAt(index))
            }
        }
        if (view is RecyclerView) {
            view.adapter?.notifyDataSetChanged()
        }
    }
}
