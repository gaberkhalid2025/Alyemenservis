package com.example.ui.screens.register.status

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class JoinStatusUseCaseTest {
    
    private val useCase = JoinStatusUseCase()
    
    @Test
    fun `determineStatus - empty phone returns NoRequest`() {
        val result = useCase.determineStatus(
            joinPhone = "",
            pendingProviders = emptyList(),
            providers = emptyList(),
            stores = emptyList(),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList()
        )
        assertTrue(result is JoinStatus.NoRequest)
    }
    
    @Test
    fun `determineStatus - unknown phone returns PendingGeneric`() {
        val result = useCase.determineStatus(
            joinPhone = "779999999",
            pendingProviders = emptyList(),
            providers = emptyList(),
            stores = emptyList(),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList()
        )
        assertTrue(result is JoinStatus.PendingGeneric)
    }
    
    @Test
    fun `determineStatus - rejected provider returns Rejected`() {
        val pendingProvider = PendingProviderEntity(
            id = "test",
            name = "Test Provider",
            phone = "771234567",
            categoryId = "provider",
            area = "صنعاء",
            status = "REJECTED",
            reason = "مستندات غير واضحة"
        )
        
        val result = useCase.determineStatus(
            joinPhone = "771234567",
            pendingProviders = listOf(pendingProvider),
            providers = emptyList(),
            stores = emptyList(),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList()
        )
        
        assertTrue(result is JoinStatus.Rejected)
        assertEquals("مستندات غير واضحة", (result as JoinStatus.Rejected).reason)
    }
}
