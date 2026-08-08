package com.alexcupsa.wifithermal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alexcupsa.wifithermal.MainActivity
import com.alexcupsa.wifithermal.R

object ScanNotificationManager {

    const val CHANNEL_ID = "wifi_scan_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WiFi Scanning",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Active WiFi survey scanning"
            setShowBadge(false)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        apCount: Int = 0,
        scanning: Boolean = false,
    ): Notification {
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val status = if (scanning) "Scanning..." else "$apCount APs visible"

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("WiFi Thermal Scanner")
            .setContentText(status)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
