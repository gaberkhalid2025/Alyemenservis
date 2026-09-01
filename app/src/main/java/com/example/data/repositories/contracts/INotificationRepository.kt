package com.example.data.repositories.contracts

import com.example.data.NotificationEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    fun clearListeners()
    suspend fun sendNotification(notification: NotificationEntity): AppResult<String>
    fun observeUserNotifications(userId: String, phone: String, role: String = "CLIENT"): Flow<List<NotificationEntity>>
    fun observeAdminNotifications(): Flow<List<NotificationEntity>>
    suspend fun markAsRead(notificationId: String): AppResult<Unit>
    suspend fun markAllAsRead(notificationIds: List<String>): AppResult<Unit>
    suspend fun deleteNotification(notificationId: String): AppResult<Unit>
}
