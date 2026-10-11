package ru.ytkab0bp.slicebeam.utils

import java.util.Random

open class RandomUtils {
    companion object {
        @JvmField
        val RANDOM = Random()

        @JvmStatic
        fun randomf(min: Float, max: Float): Float = min + RANDOM.nextFloat() * (max - min)

        @JvmStatic
        fun randoml(min: Long, max: Long): Long = (min + RANDOM.nextDouble() * (max - min)).toLong()
    }
}
