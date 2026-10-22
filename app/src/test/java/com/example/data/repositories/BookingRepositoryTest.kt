package com.example.data.repositories

import com.example.data.BookingEntity
import com.example.utils.CoroutineTestRule
import com.example.utils.TestMockFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRepositoryTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var repository: IBookingRepository

    @Before
    fun setup() {
        repository = TestMockFactory.createBookingRepository()
    }

    @Test
    fun `test createBooking - adds booking and notifies success`() = runTest {
        val booking = BookingEntity(
            id = "b1",
            customerName = "أحمد علي",
            customerPhone = "771234567",
            status = "PENDING"
        )
        var callbackSuccess = false

        repository.createBooking(
            booking = booking,
            rawPasswordPin = "1234",
            onSuccess = { created ->
                callbackSuccess = true
                assertEquals("b1", created.id)
                assertEquals("أحمد علي", created.customerName)
            },
            onError = { fail("Creation should not fail: $it") }
        )

        assertTrue(callbackSuccess)
        val cached = repository.cachedBookings.value
        assertEquals(1, cached.size)
        assertEquals("b1", cached[0].id)
    }

    @Test
    fun `test updateBookingStatus - updates status correctly`() = runTest {
        val booking = BookingEntity(id = "b2", customerName = "محمد", status = "PENDING")
        repository.createBooking(booking, onSuccess = {}, onError = {})

        var updateSuccess = false
        repository.updateBookingStatus(
            bookingId = "b2",
            newStatus = "ACCEPTED",
            onSuccess = { updateSuccess = true },
            onError = { fail("Update should not fail: $it") }
        )

        assertTrue(updateSuccess)
        val cached = repository.cachedBookings.value
        assertEquals("ACCEPTED", cached.first { it.id == "b2" }.status)
    }

    @Test
    fun `test cancelBookingWithSecurity - cancels booking`() = runTest {
        val booking = BookingEntity(id = "b3", customerName = "سالم", status = "ACCEPTED")
        repository.createBooking(booking, onSuccess = {}, onError = {})

        var cancelSuccess = false
        repository.cancelBookingWithSecurity(
            booking = booking,
            inputPinOrPassword = "1234",
            cancellationReason = "ظرف طارئ",
            cancelledBy = "CLIENT",
            onSuccess = { cancelSuccess = true },
            onError = { fail("Cancellation failed: $it") }
        )

        assertTrue(cancelSuccess)
        val cached = repository.cachedBookings.value
        assertEquals("CANCELLED", cached.first { it.id == "b3" }.status)
    }

    @Test
    fun `test deleteBooking - removes booking`() = runTest {
        val booking = BookingEntity(id = "b4", customerName = "خالد", status = "PENDING")
        repository.createBooking(booking, onSuccess = {}, onError = {})
        assertEquals(1, repository.cachedBookings.value.size)

        var deleteSuccess = false
        repository.deleteBooking(
            bookingId = "b4",
            onSuccess = { deleteSuccess = true },
            onError = { fail("Delete failed") }
        )

        assertTrue(deleteSuccess)
        assertEquals(0, repository.cachedBookings.value.size)
    }

    @Test
    fun `test getUserBookings - filters correctly`() = runTest {
        val b1 = BookingEntity(id = "b_u1", clientId = "user_100", customerName = "العميل 1")
        val b2 = BookingEntity(id = "b_u2", clientId = "user_200", customerName = "العميل 2")
        repository.createBooking(b1, onSuccess = {}, onError = {})
        repository.createBooking(b2, onSuccess = {}, onError = {})

        val user1Bookings = repository.getUserBookings("user_100").first()
        assertEquals(1, user1Bookings.size)
        assertEquals("b_u1", user1Bookings[0].id)
    }

    @Test
    fun `test getProviderBookings - filters by provider`() = runTest {
        val b1 = BookingEntity(id = "b_p1", providerId = "prov_A")
        val b2 = BookingEntity(id = "b_p2", providerId = "prov_B")
        repository.createBooking(b1, onSuccess = {}, onError = {})
        repository.createBooking(b2, onSuccess = {}, onError = {})

        val provABookings = repository.getProviderBookings("prov_A").first()
        assertEquals(1, provABookings.size)
        assertEquals("b_p1", provABookings[0].id)
    }
}
