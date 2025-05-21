package com.example.mad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Check if PIN is enabled and not locked out
        val prefsHelper = PrefsHelper(context)
        if (prefsHelper.getAppPin() != null && !prefsHelper.isPinLockedOut()) {
            NotificationHelper(context).showDailyReminder()
        }
    }
}