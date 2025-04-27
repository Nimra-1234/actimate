package com.example.actimate.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.actimate.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val GROUP_KEY = "com.example.actimate.services"
        const val CHANNEL_ID = "ServicesChannel"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ActiMate Services",
            NotificationManager.IMPORTANCE_DEFAULT // Changed from LOW to DEFAULT
        ).apply {
            description = "Notifications for ActiMate's active services"
        }
        notificationManager.createNotificationChannel(channel)
        android.util.Log.d("NotificationHelper", "NotificationChannel $CHANNEL_ID created")
    }

    fun createServiceNotification(serviceName: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID) // Use NotificationCompat.Builder
            .setContentTitle("$serviceName is running")
            .setContentText("This service is active.") // Add content text
            .setSmallIcon(R.drawable.ic_notification) // Replace with custom icon if available
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Ensure visibility
            .setGroup(GROUP_KEY)
            .build()
    }
}
