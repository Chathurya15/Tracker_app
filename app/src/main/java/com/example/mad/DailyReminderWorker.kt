package com.example.mad

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            NotificationHelper(applicationContext).showDailyReminder()
            Result.success()
        } catch (e: Exception) {
            // Retry with backoff if failed
            Result.retry()
        }
    }
}

fun scheduleDailyReminder(context: Context) {
    val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
        24, TimeUnit.HOURS,
        15, TimeUnit.MINUTES // More flexible timing
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_reminder",
        ExistingPeriodicWorkPolicy.UPDATE,
        dailyRequest
    )
}