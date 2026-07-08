package com.carryrao.vnet

import android.content.Context
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

class VnetConnection(
    private val context: Context,
    private val vpnService: VpnService,
    private val serverAddr: String,
    private val localIP: String,
    private val prefixLength: Int,
    private val secretKey: String,
    private val tunFd: ParcelFileDescriptor,
    private val onStatusChanged: (String) -> Unit
) {
    companion object {
        private const val TAG = "CLIENT"
        private const val MTU = 1500
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .pingInterval(10, java.util.concurrent.TimeUnit.SECONDS)
        .socketFactory(ProtectedSocketFactory(vpnService))
        .build()
    private val tunInput = FileInputStream(tunFd.fileDescriptor)
    private val tunOutput = FileOutputStream(tunFd.fileDescriptor)

    @Volatile
    private var connected = false
    @Volatile
    private var stopped = false

    fun start() {
        VnetLogger.clear()
        VnetLogger.log(TAG, "客户端启动 virtIp=$localIP/$prefixLength")
        VnetStats.start()
        scope.launch { connectLoop() }
    }

    fun stop() {
        VnetLogger.log(TAG, "客户端停止")
        stopped = true
        webSocket?.close(1000, "stop")
        VnetStats.stop()
        scope.cancel()
        try { tunInput.close() } catch (_: Exception) {}
        try { tunOutput.close() } catch (_: Exception) {}
    }

    private suspend fun connectLoop() {
        while (!stopped) {
            try {
                connect()
            } catch (e: Exception) {
                if (!stopped) {
                    VnetLogger.log(TAG, "连接失败: ${e.message}")
                    VnetStats.setLatency(0)
                    onStatusChanged(context.getString(R.string.status_connect_fail, 2))
                    delay(2000)
                }
            }
        }
    }

    private fun connect() {
        val url = "$serverAddr?key=$secretKey&ip=$localIP"
        VnetLogger.log(TAG, "正在连接 $serverAddr")
        onStatusChanged(context.getString(R.string.status_connecting))

        val request = Request.Builder().url(url).build()
        val latch = java.util.concurrent.CountDownLatch(1)
        val connectStart = System.currentTimeMillis()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val rtt = (System.currentTimeMillis() - connectStart).toInt()
                VnetStats.setLatency(rtt)
                VnetLogger.log(TAG, "已连接 virtIp=$localIP 延迟=${rtt}ms")
                connected = true
                onStatusChanged(context.getString(R.string.status_connected, localIP))
                this@VnetConnection.webSocket = webSocket
                latch.countDown()
                startTunReadLoop(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    synchronized(tunOutput) {
                        tunOutput.write(bytes.toByteArray())
                        tunOutput.flush()
                    }
                    VnetStats.addRx(bytes.size)
                    VnetLogger.log(TAG, "WS->TUN len=${bytes.size}")
                } catch (e: Exception) {
                    VnetLogger.log(TAG, "TUN写入失败: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                VnetLogger.log(TAG, "连接错误: ${t.message}")
                handleDisconnect()
            }
        })

        latch.await()
        while (connected && !stopped) {
            Thread.sleep(100)
        }
    }

    private fun handleDisconnect() {
        connected = false
        webSocket = null
        VnetStats.setLatency(0)
        if (!stopped) {
            VnetLogger.log(TAG, "断开连接，1s后重连...")
            onStatusChanged(context.getString(R.string.status_disconnected_retry, 1))
        }
    }

    private fun startTunReadLoop(ws: WebSocket) {
        Thread {
            val buf = ByteArray(MTU)
            while (connected && !stopped) {
                try {
                    val n = tunInput.read(buf)
                    if (n > 0) {
                        val data = buf.copyOf(n).toByteString(0, n)
                        val sent = ws.send(data)
                        if (!sent) {
                            VnetLogger.log(TAG, "WS发送失败")
                            break
                        }
                        VnetStats.addTx(n)
                        VnetLogger.log(TAG, "TUN->WS len=$n")
                    }
                } catch (e: Exception) {
                    if (connected && !stopped) {
                        VnetLogger.log(TAG, "TUN读取失败: ${e.message}")
                        Thread.sleep(10)
                    }
                }
            }
        }.start()
    }

    private class ProtectedSocketFactory(private val vpnService: VpnService) : SocketFactory() {
        private val delegate = SocketFactory.getDefault()

        override fun createSocket(): Socket {
            val socket = delegate.createSocket()
            vpnService.protect(socket)
            return socket
        }

        override fun createSocket(host: String, port: Int): Socket {
            val socket = delegate.createSocket()
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(host, port))
            return socket
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            val socket = delegate.createSocket(localHost, localPort)
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(host, port))
            return socket
        }

        override fun createSocket(host: InetAddress, port: Int): Socket {
            val socket = delegate.createSocket()
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(host, port))
            return socket
        }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
            val socket = delegate.createSocket(localAddress, localPort)
            vpnService.protect(socket)
            socket.connect(InetSocketAddress(address, port))
            return socket
        }
    }
}
