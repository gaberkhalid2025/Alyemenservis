package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatChannelRoomEntity::class,
        ChatMessageRoomEntity::class,
        BookingRoomEntity::class,
        InstantRequestRoomEntity::class,
        RequestOfferRoomEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun bookingDao(): BookingDao
    abstract fun requestDao(): RequestDao
    abstract fun offerDao(): OfferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Explicit non-destructive migration path: ensure indices for fast local queries
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_channelId` ON `chat_messages` (`channelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookings_status` ON `bookings` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_instant_requests_status` ON `instant_requests` (`status`)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_request_offers_requestId` ON `request_offers` (`requestId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yemen_services_room_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
