package com.example.ui

import android.content.Context
import com.example.ui.*
import com.example.data.NotificationEntity

fun MainViewModel.broadcastNotification(
    title: String,
    message: String,
    targetType: String = "ALL",
    targetValue: String = "",
    context: Context? = null
) {
    this.triggerNotification(title, message, targetType, targetValue, context)
}

fun MainViewModel.showAppToast(message: String, context: Context? = null) {
    this.triggerNotification(message, context)
}
