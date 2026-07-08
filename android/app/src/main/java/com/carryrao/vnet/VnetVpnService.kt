package com.carryrao.vnet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.net.InetAddress

class VnetVpnService : VpnService() {

    companion object {
        private const val TAG = "VnetVpnService"
        private const val CHANNEL_ID = "vnet_vpn"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.carryrao.vnet.START"
        const val ACTION_STOP = "com.carryrao.vnet.STOP"
        const val EXTRA_SERVER = "server"
        const val EXTRA_IP = "ip"
        const val EXTRA_PREFIX = "prefix"
        const val EXTRA_IPV6 = "ipv6"
        const val EXTRA_PREFIX6 = "prefix6"
        const val EXTRA_KEY = "key"
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var connection: VnetConnection? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val server = intent.getStringExtra(EXTRA_SERVER) ?: ""
                val ip = intent.getStringExtra(EXTRA_IP) ?: ""
                val prefix = intent.getIntExtra(EXTRA_PREFIX, 24)
                val ipv6 = intent.getStringExtra(EXTRA_IPV6) ?: ""
                val prefix6 = intent.getIntExtra(EXTRA_PREFIX6, 64)
                val key = intent.getStringExtra(EXTRA_KEY) ?: ""
                startVpn(server, ip, prefix, ipv6, prefix6, key)
            }
        }
        return START_STICKY
    }

    private fun startVpn(server: String, ip: String, prefixLength: Int, ipv6: String, prefix6: Int, key: String) {
        connection?.stop()
        connection = null
        try { tunFd?.close() } catch (_: Exception) {}
        tunFd = null

        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.status_connecting)))

        val builder = Builder()
        builder.setSession("VNet")
        builder.addAddress(ip, prefixLength)

        val ipInt = ip.split(".").fold(0) { acc, o -> (acc shl 8) or o.toInt() }
        val mask = if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
        val network = ipInt and mask
        builder.addRoute(
            InetAddress.getByAddress(byteArrayOf(
                (network shr 24).toByte(),
                (network shr 16).toByte(),
                (network shr 8).toByte(),
                network.toByte()
            )),
            prefixLength
        )

        builder.addRoute(InetAddress.getByName("224.0.0.0"), 4)

        if (ipv6.isNotEmpty()) {
            builder.addAddress(ipv6, prefix6)
            builder.addRoute(InetAddress.getByName("::"), 0)
        }

        builder.setMtu(1500)
        builder.setBlocking(true)

        tunFd = builder.establish()
        if (tunFd == null) {
            VnetLogger.log(TAG, "TUN创建失败")
            stopVpn()
            return
        }
        val logMsg = if (ipv6.isNotEmpty()) "TUN已创建 ip=$ip/$prefixLength ipv6=$ipv6/$prefix6"
                     else "TUN已创建 ip=$ip/$prefixLength"
        VnetLogger.log(TAG, logMsg)

        connection = VnetConnection(
            context = this,
            vpnService = this,
            serverAddr = server,
            localIP = ip,
            prefixLength = prefixLength,
            secretKey = key,
            tunFd = tunFd!!,
            onStatusChanged = { status -> updateNotification(status) }
        )
        connection?.start()
    }

    private fun stopVpn() {
        connection?.stop()
        connection = null
        try { tunFd?.close() } catch (_: Exception) {}
        tunFd = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        connection?.stop()
        connection = null
        try { tunFd?.close() } catch (_: Exception) {}
        tunFd = null
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("VNet")
        .setContentText(text)
        .setOngoing(true)
        .build()

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
