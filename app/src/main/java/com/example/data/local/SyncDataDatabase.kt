package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🔄 SyncDataEntity
 * كائن تخزين بيانات المزامنة الجزئية والتغييرات المحلية في Room.
 */
@Entity(tableName = "sync_data", indices = [Index(value = ["timestamp"])])
data class SyncDataEntity(
    @PrimaryKey val key: String,
    val value: String, // JSON payload or text
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 📊 SyncDataDao
 * واجهة الاستعلام والتحكم ببيانات المزامنة.
 */
@Dao
interface SyncDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: SyncDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataList: List<SyncDataEntity>)

    @Query("SELECT * FROM sync_data WHERE key = :key")
    suspend fun get(key: String): SyncDataEntity?

    @Query("SELECT * FROM sync_data")
    fun getAllFlow(): Flow<List<SyncDataEntity>>

    @Query("SELECT * FROM sync_data")
    suspend fun getAllList(): List<SyncDataEntity>

    @Query("DELETE FROM sync_data WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM sync_data WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM sync_data")
    suspend fun clearAll()
}

/**
 * 💾 SyncDataDatabase
 * قاعدة بيانات Room الخاصة بإدارة المزامنة المحلية والسحابية.
 */
@Database(entities = [SyncDataEntity::class], version = 1, exportSchema = false)
abstract class SyncDataDatabase : RoomDatabase() {
    abstract fun syncDataDao(): SyncDataDao

    companion object {
        @Volatile
        private var INSTANCE: SyncDataDatabase? = null

        fun getInstance(context: Context): SyncDataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyncDataDatabase::class.java,
                    "sync_data_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
