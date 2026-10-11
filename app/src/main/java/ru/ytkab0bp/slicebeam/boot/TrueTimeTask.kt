package ru.ytkab0bp.slicebeam.boot

import com.instacart.truetime.TrueTimeEventListener
import com.instacart.truetime.time.TrueTimeImpl
import com.instacart.truetime.time.TrueTimeParameters
import kotlinx.coroutines.Dispatchers
import ru.ytkab0bp.slicebeam.SliceBeam
import java.net.InetAddress
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class TrueTimeTask : BootTask(Runnable {
    val initializationLatch = CountDownLatch(1)
    val trueTime = TrueTimeImpl(
        TrueTimeParameters.Builder().buildParams(),
        Dispatchers.IO,
        object : TrueTimeEventListener {
            override fun initialize(trueTimeParameters: TrueTimeParameters) {}

            override fun initializeSuccess(values: LongArray) {
                initializationLatch.countDown()
            }

            override fun initializeFailed(error: Exception) {}

            override fun nextInitializeIn(delay: Long) {}

            override fun resolvedNtpHostToIPs(host: String, addresses: List<InetAddress>) {}

            override fun lastSntpRequestAttempt(address: InetAddress) {}

            override fun sntpRequestFailed(error: Exception) {}

            override fun syncDispatcherException(error: Throwable) {}

            override fun sntpRequest(address: InetAddress) {}

            override fun sntpRequestSuccessful(address: InetAddress) {}

            override fun sntpRequestFailed(address: InetAddress, error: Exception) {}

            override fun storingTrueTime(values: LongArray) {}

            override fun returningTrueTime(date: Date) {}

            override fun returningDeviceTime() {}
        }
    )
    SliceBeam.TRUE_TIME = trueTime
    trueTime.sync()
    try {
        initializationLatch.await(300, TimeUnit.MILLISECONDS)
    } catch (_: InterruptedException) {
    }
}) {
    init {
        onWorker()
        nonCritical = true
    }
}
