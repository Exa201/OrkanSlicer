package ru.ytkab0bp.sapil

interface APIEnclosure {
    fun canExecuteRequests(): Boolean
    fun addLifecycleDestroyListener(destroyListener: LifecycleDestroyListener)
    fun removeLifecycleDestroyListener(destroyListener: LifecycleDestroyListener)

    fun interface LifecycleDestroyListener {
        fun onDestroy()
    }
}
