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
    version = 4,
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

        private fun ensureAllIndices(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            val statements = listOf(
                "CREATE INDEX IF NOT EXISTS `index_chat_channels_lastMessageTime` ON `chat_channels` (`lastMessageTime`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_channels_updatedAt` ON `chat_channels` (`updatedAt`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_channels_syncStatus` ON `chat_channels` (`syncStatus`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_channelId` ON `chat_messages` (`channelId`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_timestamp` ON `chat_messages` (`timestamp`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_senderId` ON `chat_messages` (`senderId`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_status` ON `chat_messages` (`status`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_syncStatus` ON `chat_messages` (`syncStatus`)",
                "CREATE INDEX IF NOT EXISTS `index_chat_messages_channelId_timestamp` ON `chat_messages` (`channelId`, `timestamp`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_providerId` ON `bookings` (`providerId`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_customerPhone` ON `bookings` (`customerPhone`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_status` ON `bookings` (`status`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_createdAt` ON `bookings` (`createdAt`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_scheduledAt` ON `bookings` (`scheduledAt`)",
                "CREATE INDEX IF NOT EXISTS `index_bookings_providerId_status` ON `bookings` (`providerId`, `status`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_userId` ON `instant_requests` (`userId`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_userPhone` ON `instant_requests` (`userPhone`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_userCity` ON `instant_requests` (`userCity`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_status` ON `instant_requests` (`status`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_createdAt` ON `instant_requests` (`createdAt`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_expiresAt` ON `instant_requests` (`expiresAt`)",
                "CREATE INDEX IF NOT EXISTS `index_instant_requests_requestCode` ON `instant_requests` (`requestCode`)",
                "CREATE INDEX IF NOT EXISTS `index_request_offers_requestId` ON `request_offers` (`requestId`)",
                "CREATE INDEX IF NOT EXISTS `index_request_offers_requestCode` ON `request_offers` (`requestCode`)",
                "CREATE INDEX IF NOT EXISTS `index_request_offers_technicianId` ON `request_offers` (`technicianId`)",
                "CREATE INDEX IF NOT EXISTS `index_request_offers_status` ON `request_offers` (`status`)",
                "CREATE INDEX IF NOT EXISTS `index_request_offers_createdAt` ON `request_offers` (`createdAt`)"
            )
            for (sql in statements) {
                try {
                    db.execSQL(sql)
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Index creation failed: $sql", e)
                }
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                ensureAllIndices(db)
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                ensureAllIndices(db)
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                ensureAllIndices(db)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yemen_services_room_db"
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
