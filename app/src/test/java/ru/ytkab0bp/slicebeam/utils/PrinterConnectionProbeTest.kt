package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class PrinterConnectionProbeTest {
    @Test
    fun recognizesReachableHostEvenWhenHeadIsNotAllowed() {
        ServerSocket(0).use { server ->
            val responder = thread {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    reader.readLine()
                    while (true) {
                        if (reader.readLine().isNullOrEmpty()) break
                    }
                    socket.getOutputStream().write(
                        "HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(),
                    )
                    socket.getOutputStream().flush()
                }
            }
            assertTrue(PrinterConnectionProbe.isReachable("http://127.0.0.1:${server.localPort}/", 1_000))
            responder.join(1_000)
        }
    }

    @Test
    fun reportsClosedLocalPortAsUnreachable() {
        val server = ServerSocket(0)
        val port = server.localPort
        server.close()
        assertFalse(PrinterConnectionProbe.isReachable("http://127.0.0.1:$port/", 300))
    }

    @Test
    fun rejectsNonHttpAddressWithoutThrowing() {
        assertFalse(PrinterConnectionProbe.isReachable("not a URL", 100))
    }
}
