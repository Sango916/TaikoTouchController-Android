package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Android 14+ (targetSdk 34) Foreground Service for Remote Device Communication.
 * Uses foregroundServiceType="connectedDevice" to guarantee that when the user switches
 * to a rhythm game or emulator in the foreground, this receiver process is NOT throttled,
 * frozen in cached state, or killed by the Android OS.
 */
class RemoteReceiverService : Service() {

    companion object {
        const val ACTION_START = "com.example.ACTION_START_REMOTE_RECEIVER"
        const val ACTION_STOP = "com.example.ACTION_STOP_REMOTE_RECEIVER"
        private const val CHANNEL_ID = "taiko_remote_receiver_channel"
        private const val NOTIFICATION_ID = 9022

        @Volatile
        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, RemoteReceiverService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RemoteReceiverService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()
        isServiceRunning = true
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = tr(this, "🥁 太鼓リモート受信待機中", "🥁 Taiko Remote Receiver Active")
        val content = tr(this, "ゲーム画面の裏でキー入力を高精度に受信中", "Receiving low-latency key inputs in background")

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = tr(this, "太鼓リモート通信サービス", "Taiko Remote Communication Service")
            val descriptionText = tr(this, "外部端末からのキー入力をバックグラウンドで維持します", "Maintains remote key input reception in background")
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
