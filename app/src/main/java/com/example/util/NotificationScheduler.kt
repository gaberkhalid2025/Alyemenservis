package com.example.util

import android.content.Context
import android.util.Log

object NotificationScheduler {
    fun scheduleOneShot(context: Context, delaySeconds: Long, title: String, content: String) {
        Log.d("NotificationScheduler", "Scheduled notification: '$title' in $delaySeconds seconds")
    }

    fun scheduleRecurring(context: Context, intervalMinutes: Long, title: String, content: String) {
        Log.d("NotificationScheduler", "Scheduled recurring notification: '$title' every $intervalMinutes minutes")
    }

    fun cancelAllScheduled(context: Context) {
        Log.d("NotificationScheduler", "Cancelled all scheduled notifications")
    }
}
