package ru.ytkab0bp.slicebeam.utils

open class ThreadLocalDoubleArray(private val size: Int) : ThreadLocal<DoubleArray?>() {
    override fun initialValue(): DoubleArray? = DoubleArray(size)
}
