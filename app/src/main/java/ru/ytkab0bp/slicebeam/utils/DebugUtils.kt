package ru.ytkab0bp.slicebeam.utils

import ru.ytkab0bp.slicebeam.BuildConfig

open class DebugUtils {
    companion object {
        @JvmStatic
        fun assertTrue(value: Boolean) = throwIfNot(value)

        @JvmStatic
        fun assertFalse(value: Boolean) = throwIfNot(!value)

        private fun throwIfNot(value: Boolean) {
            if (BuildConfig.DEBUG && !value) throw AssertionError("Assert failed")
        }
    }
}
