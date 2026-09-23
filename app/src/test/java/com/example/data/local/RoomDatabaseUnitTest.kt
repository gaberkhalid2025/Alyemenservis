package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 🗄️ RoomDatabaseUnitTest
 * Comprehensive unit test suite for local Room Database layer (AppDatabase, BookingDao, RequestDao, OfferDao, ChatDao),
 * ensuring insertions, updates, queries, deletions, and edge/failure handling operate reliably.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var bookingDao: BookingDao
    private lateinit var requestDao: RequestDao
    private lateinit var offerDao: OfferDao
    private lateinit var chatDao: ChatDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        bookingDao = db.bookingDao()
        requestDao = db.requestDao()
        offerDao = db.offerDao()
        chatDao = db.chatDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ==========================================
    // 1. BOOKING DAO TESTS
    // ==========================================

    @Test
    fun `test insert and get booking by id`() = runBlocking {
        val booking = BookingRoomEntity(
            id = "bk_001",
            customerName = "أحمد محمد",
            customerPhone = "+967771234567",
            customerArea = "صنعاء - حدة",
            serviceType = "صيانة كهرباء",
            providerId = "prov_100",
            providerName = "المهندس علي",
            dateString = "2026-09-25",
            timeString = "10:00 AM",
            status = "PENDING",
            pinCode = "1234",
            bookingNumber = "BK-9901",
            totalAmount = 15000.0,
            advancePayment = 5000.0,
            paymentStatus = "UNPAID",
            scheduledAt = 1758000000000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        bookingDao.insertBooking(booking)
        val loaded = bookingDao.getBookingById("bk_001")

        assertNotNull(loaded)
        assertEquals("أحمد محمد", loaded?.customerName)
        assertEquals("+967771234567", loaded?.customerPhone)
        assertEquals("صيانة كهرباء", loaded?.serviceType)
        assertEquals("PENDING", loaded?.status)
        assertEquals(15000.0, loaded?.totalAmount ?: 0.0, 0.001)
    }

    @Test
    fun `test update booking status and timestamps`() = runBlocking {
        val booking = BookingRoomEntity(
            id = "bk_002",
            customerName = "سامي سالم",
            customerPhone = "+967772223344",
            customerArea = "عدن - المعلا",
            serviceType = "سباكة",
            providerId = "prov_200",
            providerName = "فني السباكة",
            dateString = "2026-09-26",
            timeString = "02:00 PM",
            status = "PENDING",
            pinCode = "5678",
            bookingNumber = "BK-9902",
            totalAmount = 20000.0,
            advancePayment = 0.0,
            paymentStatus = "UNPAID",
            scheduledAt = 1758000000000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        bookingDao.insertBooking(booking)

        val updatedTime = System.currentTimeMillis() + 5000
        bookingDao.updateBookingStatus(id = "bk_002", status = "ACCEPTED", updatedAt = updatedTime)

        val loaded = bookingDao.getBookingById("bk_002")
        assertNotNull(loaded)
        assertEquals("ACCEPTED", loaded?.status)
        assertEquals(updatedTime, loaded?.updatedAt)
    }

    @Test
    fun `test delete booking removes entity completely`() = runBlocking {
        val booking = BookingRoomEntity(
            id = "bk_to_delete",
            customerName = "حسين عبدالله",
            customerPhone = "+967773334455",
            customerArea = "تعز",
            serviceType = "تكييف",
            providerId = "prov_300",
            providerName = "فني تكييف",
            dateString = "2026-09-27",
            timeString = "04:00 PM",
            status = "CANCELLED",
            pinCode = "9999",
            bookingNumber = "BK-9903",
            totalAmount = 10000.0,
            advancePayment = 0.0,
            paymentStatus = "CANCELLED",
            scheduledAt = 1758000000000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        bookingDao.insertBooking(booking)
        assertNotNull(bookingDao.getBookingById("bk_to_delete"))

        bookingDao.deleteBooking("bk_to_delete")
        val afterDelete = bookingDao.getBookingById("bk_to_delete")
        assertNull(afterDelete)
    }

    @Test
    fun `test onConflict replace updates existing booking entity`() = runBlocking {
        val original = BookingRoomEntity(
            id = "bk_conflict_test",
            customerName = "خالد عمر",
            customerPhone = "+967774445566",
            customerArea = "إب",
            serviceType = "نجارة",
            providerId = "prov_400",
            providerName = "معلم نجارة",
            dateString = "2026-09-28",
            timeString = "11:00 AM",
            status = "PENDING",
            pinCode = "1111",
            bookingNumber = "BK-9904",
            totalAmount = 8000.0,
            advancePayment = 0.0,
            paymentStatus = "UNPAID",
            scheduledAt = 1758000000000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        bookingDao.insertBooking(original)

        val replacement = original.copy(
            customerName = "خالد عمر المعدل",
            totalAmount = 12000.0,
            status = "COMPLETED"
        )
        bookingDao.insertBooking(replacement)

        val result = bookingDao.getBookingById("bk_conflict_test")
        assertEquals("خالد عمر المعدل", result?.customerName)
        assertEquals(12000.0, result?.totalAmount ?: 0.0, 0.001)
        assertEquals("COMPLETED", result?.status)
    }

    // ==========================================
    // 2. INSTANT REQUEST & OFFER DAO TESTS
    // ==========================================

    @Test
    fun `test insert and query instant request and offers`() = runBlocking {
        val request = InstantRequestRoomEntity(
            id = "req_101",
            requestCode = "REQ-8877",
            secretPin = "4321",
            userId = "user_55",
            userName = "طارق حسان",
            userPhone = "+967775556677",
            userCity = "صنعاء",
            serviceTitle = "إصلاح عطل كهربائي عاجل",
            description = "انقطاع مفاجئ في لوحة التوزيع الرئيسية",
            status = "OPEN",
            acceptedPrice = 0.0,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 3600000L,
            offersCount = 2
        )
        requestDao.insertRequest(request)

        val offer1 = RequestOfferRoomEntity(
            id = "off_1",
            requestId = "req_101",
            requestCode = "REQ-8877",
            technicianId = "tech_1",
            technicianName = "فني 1",
            technicianPhone = "+967771111111",
            price = 5000.0,
            estimatedArrivalTime = "15 دقيقة",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val offer2 = RequestOfferRoomEntity(
            id = "off_2",
            requestId = "req_101",
            requestCode = "REQ-8877",
            technicianId = "tech_2",
            technicianName = "فني 2",
            technicianPhone = "+967772222222",
            price = 4500.0,
            estimatedArrivalTime = "20 دقيقة",
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        offerDao.insertOffers(listOf(offer1, offer2))

        val loadedReq = requestDao.getRequestById("req_101")
        assertNotNull(loadedReq)
        assertEquals("REQ-8877", loadedReq?.requestCode)

        val offers = offerDao.getOffersListForRequest("req_101")
        assertEquals(2, offers.size)

        offerDao.updateOfferStatus("off_2", "ACCEPTED")
        requestDao.updateRequestStatus("req_101", "ACCEPTED")

        val updatedReq = requestDao.getRequestById("req_101")
        assertEquals("ACCEPTED", updatedReq?.status)
    }

    // ==========================================
    // 3. CHAT DAO & MESSAGE CASCADE TESTS
    // ==========================================

    @Test
    fun `test chat channel and message transactions`() = runBlocking {
        val channel = ChatChannelRoomEntity(
            id = "chan_99",
            title = "محادثة الصيانة",
            type = "DIRECT",
            participantsJson = "[\"user_1\", \"prov_1\"]",
            lastMessage = "السلام عليكم",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageSenderId = "user_1",
            unreadCountJson = "{}",
            syncStatus = "SYNCED",
            updatedAt = System.currentTimeMillis()
        )

        val msg1 = ChatMessageRoomEntity(
            id = "msg_1",
            channelId = "chan_99",
            senderId = "user_1",
            senderName = "العميل",
            senderPhoto = "",
            message = "السلام عليكم، متى الموعد؟",
            mediaType = "TEXT",
            mediaUrl = "",
            status = "SENT",
            isEncrypted = false,
            timestamp = 1000L,
            syncStatus = "SYNCED"
        )
        val msg2 = ChatMessageRoomEntity(
            id = "msg_2",
            channelId = "chan_99",
            senderId = "prov_1",
            senderName = "الفني",
            senderPhoto = "",
            message = "وعليكم السلام، خلال ساعة بإذن الله",
            mediaType = "TEXT",
            mediaUrl = "",
            status = "DELIVERED",
            isEncrypted = false,
            timestamp = 2000L,
            syncStatus = "SYNCED"
        )

        chatDao.replaceChannelWithMessages(channel, listOf(msg1, msg2))

        val loadedChannel = chatDao.getChannelById("chan_99")
        assertNotNull(loadedChannel)
        assertEquals("محادثة الصيانة", loadedChannel?.title)

        val messages = chatDao.getMessagesList("chan_99")
        assertEquals(2, messages.size)
        assertEquals("msg_1", messages[0].id)
        assertEquals("msg_2", messages[1].id)

        chatDao.updateMessageStatus("msg_1", "READ")
        val updatedMessages = chatDao.getMessagesList("chan_99")
        assertEquals("READ", updatedMessages.find { it.id == "msg_1" }?.status)

        // Clear messages
        chatDao.clearChannelMessages("chan_99")
        val emptyMessages = chatDao.getMessagesList("chan_99")
        assertTrue(emptyMessages.isEmpty())
    }
}
