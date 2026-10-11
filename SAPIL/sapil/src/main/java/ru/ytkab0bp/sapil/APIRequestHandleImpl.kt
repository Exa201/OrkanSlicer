package ru.ytkab0bp.sapil

import java.util.concurrent.Future

internal class APIRequestHandleImpl(private val future: Future<*>) : APIRequestHandle {
    override fun isRunning(): Boolean = !future.isDone

    override fun cancel() {
        future.cancel(true)
    }
}
