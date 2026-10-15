package ru.ytkab0bp.slicebeam.boot

import android.util.Log
import android.util.SparseBooleanArray
import ru.ytkab0bp.slicebeam.BuildConfig
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Runs startup tasks after their declared predecessors have completed. */
open class AppBoot {
    companion object {
        private const val TAG = "boot"

        // Startup work is dependency-aware but finite; two workers avoid a thread per task
        // and leave CPU and memory headroom on entry-level phones.
        private var executor: ExecutorService? = Executors.newFixedThreadPool(2)
        private var tasks: List<BootTask>? = null
        private var pendingMain: MutableList<Runnable>? = ArrayList()
        private var pendingTasks: MutableList<BootTask>? = ArrayList()
        private var completed: SparseBooleanArray? = SparseBooleanArray()
        private var latch: CountDownLatch? = null

        @JvmStatic
        fun run(tasks: List<BootTask>) {
            val start = System.currentTimeMillis()
            this.tasks = tasks
            var criticalTaskCount = tasks.size
            for (task in tasks) {
                if (task.nonCritical) {
                    if (!task.workerThread) {
                        throw IllegalArgumentException("Can't schedule non-critical task on main thread")
                    }
                    criticalTaskCount--
                }
            }
            val startupLatch = CountDownLatch(criticalTaskCount)
            latch = startupLatch

            tasks.forEachIndexed { index, task ->
                task.index = index
                tryRunTask(task, fromMain = true, isContinue = false)
            }
            try {
                while (!startupLatch.await(10, TimeUnit.MILLISECONDS)) {
                    val queuedMainTasks = pendingMain ?: break
                    if (queuedMainTasks.isNotEmpty()) {
                        val readyTasks = ArrayList(queuedMainTasks)
                        readyTasks.forEach { it.run() }
                        queuedMainTasks.removeAll(readyTasks)
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Boot in ${System.currentTimeMillis() - start}ms")
                }
                tryShutdown()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        private fun tryShutdown() {
            val currentTasks = tasks ?: return
            val currentCompleted = completed ?: return
            if (currentCompleted.size() == currentTasks.size) {
                executor?.shutdown()
                executor = null
                tasks = null
                pendingMain = null
                pendingTasks = null
                completed = null
                latch = null
            }
        }

        private fun tryRunTask(task: BootTask, fromMain: Boolean, isContinue: Boolean) {
            if (checkDependencies(task.dependencies)) {
                val runnable = Runnable {
                    try {
                        task.run.run()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error while executing boot task", e)
                    }
                    completed?.put(task.index, true)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Finish $task")
                    }
                    if (!task.nonCritical) {
                        latch?.countDown()
                    } else {
                        tryShutdown()
                    }

                    if (!isContinue) {
                        continueTasks(fromMain)
                    }
                }
                if (task.workerThread) {
                    executor!!.submit(runnable)
                } else if (fromMain) {
                    runnable.run()
                } else {
                    pendingMain?.add(runnable)
                }
            } else {
                pendingTasks?.add(task)
            }
        }

        private fun continueTasks(fromMain: Boolean) {
            val waitingTasks = pendingTasks ?: return
            val iterator = waitingTasks.iterator()
            while (iterator.hasNext()) {
                val task = iterator.next()
                if (checkDependencies(task.dependencies)) {
                    tryRunTask(task, fromMain, isContinue = true)
                    iterator.remove()
                }
            }
        }

        private fun checkDependencies(dependencies: List<Class<*>>): Boolean {
            if (dependencies.isEmpty()) return true
            val currentTasks = tasks ?: return false
            val currentCompleted = completed ?: return false
            for (index in currentTasks.indices) {
                if (dependencies.contains(currentTasks[index].javaClass) && !currentCompleted.get(index)) {
                    return false
                }
            }
            return true
        }
    }
}
