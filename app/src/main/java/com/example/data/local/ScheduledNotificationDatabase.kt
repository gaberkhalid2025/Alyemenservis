package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🔔 ScheduledNotificationEntity
 * نموذج تخزين الإشعارات المجدولة محلياً في Room.
 */
@Entity(tableName = "scheduled_notifications", indices = [Index(value = ["scheduledTime"]), Index(value = ["bookingId"])])
data class ScheduledNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val scheduledTime: Long,
    val isActive: Boolean = true,
    val type: String = "GENERAL", // "BOOKING_REMINDER", "PROMOTION", "GENERAL", "SYSTEM"
    val bookingId: String = ""
)

/**
 * 📊 ScheduledNotificationDao
 * واجهة التعامل مع الإشعارات المجدولة في قاعدة البيانات المحلية.
 */
@Dao
interface ScheduledNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: ScheduledNotificationEntity)

    @Query("SELECT * FROM scheduled_notifications WHERE isActive = 1 ORDER BY scheduledTime ASC")
    fun getActiveNotificationsFlow(): Flow<List<ScheduledNotificationEntity>>

    @Query("SELECT * FROM scheduled_notifications WHERE isActive = 1 ORDER BY scheduledTime ASC")
    suspend fun getActiveNotificationsList(): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE scheduledTime <= :currentTime AND isActive = 1")
    suspend fun getPendingDueNotifications(currentTime: Long): List<ScheduledNotificationEntity>

    @Query("UPDATE scheduled_notifications SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM scheduled_notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM scheduled_notifications WHERE bookingId = :bookingId")
    suspend fun deleteByBookingId(bookingId: String)

    @Query("DELETE FROM scheduled_notifications")
    suspend fun clearAll()
}

/**
 * 💾 ScheduledNotificationDatabase
 * قاعدة بيانات Room الخاصة بجدولة الإشعارات.
 */
@Database(entities = [ScheduledNotificationEntity::class], version = 1, exportSchema = false)
abstract class ScheduledNotificationDatabase : RoomDatabase() {
    abstract fun scheduledNotificationDao(): ScheduledNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduledNotificationDatabase? = null

        fun getInstance(context: Context): ScheduledNotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduledNotificationDatabase::class.java,
                    "scheduled_notifications_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
