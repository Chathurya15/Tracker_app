package com.example.mad

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Random

class NotificationHelper(private val context: Context) {
    private val channelId = "budget_alerts"
    private val dailyReminderChannelId = "daily_reminders"
    private val random = Random()

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Budget alerts channel (high importance)
            val budgetChannel = NotificationChannel(
                channelId,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Budget warning notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            // Daily reminder channel
            val reminderChannel = NotificationChannel(
                dailyReminderChannelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily expense reminder notifications"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showBudgetWarning(percentageUsed: Int) {
        if (!hasNotificationPermission()) {
            Log.w("NotificationHelper", "Notification permission not granted")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w("NotificationHelper", "Notifications disabled by user")
            return
        }

        val message = when {
            percentageUsed >= 100 -> "Budget exceeded by ${percentageUsed - 100}%!"
            percentageUsed >= 90 -> "Budget almost used! ($percentageUsed%)"
            percentageUsed >= 75 -> "Budget $percentageUsed% used - be careful!"
            else -> return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(random.nextInt(10000), notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show notification", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun showDailyReminder() {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, dailyReminderChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Expense Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1002, notification)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }
}
