package ru.ytkab0bp.eventbus

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.RestrictTo
import java.util.ConcurrentModificationException

class EventBus private constructor(key: String) {
    @JvmField
    protected var handlers: MutableList<Any> = ArrayList()

    @JvmField
    protected var mKey: String = key

    @JvmField
    protected var handler: Handler? = null

    private var handlerThread: HandlerThread? = null

    init {
        initialize()
    }

    private fun initialize() {
        val thread = HandlerThread("eventbus_$mKey", HandlerThread.NORM_PRIORITY)
        thread.start()
        handlerThread = thread
        handler = Handler(thread.looper)
    }

    fun removeCallbacksFromThread(runnable: Runnable) {
        handler?.removeCallbacks(runnable)
    }

    fun postOnThreadDelayed(runnable: Runnable, delayMillis: Long) {
        handler?.postDelayed(runnable, delayMillis)
    }

    fun postOnThread(runnable: Runnable) {
        handler?.post(runnable)
    }

    fun registerListener(listener: Any) {
        check(mImpls.isNotEmpty()) {
            "No implementations registered, please use registerImpl(...)!"
        }
        handlers.add(listener)
    }

    fun unregisterListener(listener: Any) {
        handlers.remove(listener)
    }

    fun fireEvent(event: Any?) {
        val eventHandler = handler
        if (eventHandler != null && Looper.myLooper() != eventHandler.looper) {
            eventHandler.post { fireEvent0(event, useClone = false, startIndex = 0) }
        } else {
            fireEvent0(event, useClone = false, startIndex = 0)
        }
    }

    fun release() {
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    private fun fireEvent0(event: Any?, useClone: Boolean, startIndex: Int) {
        requireNotNull(event) { "Event should not be null!" }

        val listeners = if (useClone) ArrayList(handlers) else handlers
        val iterator = listeners.iterator()
        var index = startIndex
        while (iterator.hasNext()) {
            val listener = try {
                iterator.next()
            } catch (_: ConcurrentModificationException) {
                fireEvent0(event, useClone = true, startIndex = index)
                return
            }

            try {
                for (implementation in mImpls) {
                    implementation.onEvent(listener, event)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Exception occurred while firing event", exception)
            }
            index++
        }
    }

    companion object {
        private const val TAG = "event_bus"
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
        private val mImpls: MutableList<EventBusListenerImpl> = ArrayList()

        @JvmStatic
        fun newBus(key: String): EventBus = EventBus(key)

        /** Returns whether the registered implementations allow parent-event dispatch. */
        @JvmStatic
        fun canFire(clz: Class<*>, event: Any?): Boolean {
            var canFireParent = false
            for (implementation in mImpls) {
                if (implementation.canFireParent(clz)) {
                    canFireParent = true
                    break
                }
            }
            return if (canFireParent) clz.isInstance(event) else event?.javaClass == clz
        }

        @JvmStatic
        fun registerImpl(context: Context) {
            registerImpl(context.packageName)
        }

        /** Registers the generated implementation for an application or library package. */
        @JvmStatic
        fun registerImpl(implId: String) {
            try {
                val implementationClass = "ru.ytkab0bp.eventbus.impl." + implId.replace(".", "_Z9_")
                val implementation = Class.forName(implementationClass)
                    .getDeclaredConstructor()
                    .newInstance() as EventBusListenerImpl
                mImpls.add(implementation)
            } catch (exception: ReflectiveOperationException) {
                throw RuntimeException("Can't load implementation", exception)
            }
        }

        /** Posts a callback to the main thread. */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun postOnUiThread(runnable: Runnable) {
            MAIN_HANDLER.post(runnable)
        }
    }
}
