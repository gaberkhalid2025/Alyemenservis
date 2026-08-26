package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🎨 ColorSyncEntity
 * نموذج تخزين إعدادات وتفضيلات ألوان التطبيق في Room.
 */
@Entity(tableName = "color_sync_data")
data class ColorSyncEntity(
    @PrimaryKey val key: String,
    val value: String, // JSON payload representing ColorScheme or PersonalColors or SyncLog
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 📊 ColorSyncDao
 * واجهة التعامل مع ألوان وسمات التطبيق في Room.
 */
@Dao
interface ColorSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: ColorSyncEntity)

    @Query("SELECT * FROM color_sync_data WHERE key = :key")
    suspend fun getByKey(key: String): ColorSyncEntity?

    @Query("SELECT * FROM color_sync_data WHERE key = :key")
    fun getByKeyFlow(key: String): Flow<ColorSyncEntity?>

    @Query("SELECT * FROM color_sync_data")
    fun getAllFlow(): Flow<List<ColorSyncEntity>>

    @Query("DELETE FROM color_sync_data WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM color_sync_data")
    suspend fun clearAll()
}

/**
 * 💾 ColorSyncDatabase
 * قاعدة بيانات Room لمزامنة ألوان وسجلات سمات التطبيق.
 */
@Database(entities = [ColorSyncEntity::class], version = 1, exportSchema = false)
abstract class ColorSyncDatabase : RoomDatabase() {
    abstract fun colorSyncDao(): ColorSyncDao

    companion object {
        @Volatile
        private var INSTANCE: ColorSyncDatabase? = null

        fun getInstance(context: Context): ColorSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ColorSyncDatabase::class.java,
                    "color_sync_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
