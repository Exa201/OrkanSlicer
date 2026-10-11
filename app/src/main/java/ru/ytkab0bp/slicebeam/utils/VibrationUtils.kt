package ru.ytkab0bp.slicebeam.utils

import android.content.Context
import android.os.Build
import android.os.Vibrator

open class VibrationUtils {
    companion object {
        private var vibrator: Vibrator? = null

        @JvmStatic
        fun init(context: Context?) {
            vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }

        @JvmStatic
        fun hasAmplitudeControl(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator!!.hasAmplitudeControl()
    }
}
