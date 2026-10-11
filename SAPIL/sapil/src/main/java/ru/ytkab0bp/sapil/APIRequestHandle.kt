package ru.ytkab0bp.sapil

interface APIRequestHandle {
    fun isRunning(): Boolean
    fun cancel()
}
