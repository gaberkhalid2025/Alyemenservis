package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 📦 OfflineRequestEntity
 * نموذج الكائن الخاص بطلبات وضع الأوفلاين للتخزين في قاعدة بيانات Room.
 */
@Entity(tableName = "offline_requests", indices = [Index(value = ["status"]), Index(value = ["priority"])])
data class OfflineRequestEntity(
    @PrimaryKey val id: String,
    val type: String,
    val data: String, // JSON representation of map payload
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = 3,
    val retryCount: Int = 0,
    val status: String = "PENDING"
)

/**
 * 📊 OfflineRequestDao
 * واجهة التعامل المباشر مع جدول offline_requests في Room.
 */
@Dao
interface OfflineRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: OfflineRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<OfflineRequestEntity>)

    @Query("SELECT * FROM offline_requests WHERE status = 'PENDING' ORDER BY priority ASC, timestamp ASC")
    fun getPendingRequests(): Flow<List<OfflineRequestEntity>>

    @Query("SELECT * FROM offline_requests WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY priority ASC, timestamp ASC")
    suspend fun getPendingOrFailedRequestsList(): List<OfflineRequestEntity>

    @Query("SELECT * FROM offline_requests ORDER BY priority ASC")
    suspend fun getAllRequestsList(): List<OfflineRequestEntity>

    @Query("UPDATE offline_requests SET status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, retryCount: Int = 0)

    @Query("DELETE FROM offline_requests WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM offline_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM offline_requests")
    suspend fun clearAll()
}

/**
 * 💾 OfflineRequestDatabase
 * قاعدة بيانات Room الخاصة بطابور العمليات غير المتصلة (Offline Queue).
 */
@Database(entities = [OfflineRequestEntity::class], version = 1, exportSchema = false)
abstract class OfflineRequestDatabase : RoomDatabase() {
    abstract fun offlineRequestDao(): OfflineRequestDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineRequestDatabase? = null

        fun getInstance(context: Context): OfflineRequestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineRequestDatabase::class.java,
                    "offline_requests_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
