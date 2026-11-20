package com.example

import com.example.data.CategoryEntity
import com.example.data.PropertyEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.domain.entities.ProductItemEntity
import com.example.ui.helpers.AppPreferenceHelper
import com.example.ui.screens.register.status.JoinStatus
import com.example.ui.screens.register.status.JoinStatusUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardsAndSectorRoutingTest {

    private val useCase = JoinStatusUseCase()

    @Test
    fun commercialMallWithMarkazInNameIsNotHijackedAsMedical() {
        val mallStore = StoreEntity(
            id = "store_mall_1",
            sectionId = "stores",
            name = "مركز السعيد التجاري للإلكترونيات",
            phone = "777100200",
            ownerId = "777100200",
            medicalLicenseNo = "",
            isActive = true,
            isApproved = true
        )

        val status = useCase.determineStatus(
            joinPhone = "777100200",
            pendingProviders = emptyList(),
            providers = emptyList(),
            stores = listOf(mallStore),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList()
        )

        assertTrue(status is JoinStatus.ActiveStore)
        assertEquals("stores", (status as JoinStatus.ActiveStore).businessType)
    }

    @Test
    fun medicalStoreWithLicenseOrMedicalSectionRoutesToMedical() {
        val medicalStore = StoreEntity(
            id = "med_store_1",
            sectionId = "stores",
            name = "مركز الأمل الحديث",
            phone = "777200300",
            ownerId = "777200300",
            medicalLicenseNo = "MED-2026-99",
            isActive = true,
            isApproved = true
        )

        val status = useCase.determineStatus(
            joinPhone = "+967777200300",
            pendingProviders = emptyList(),
            providers = emptyList(),
            stores = listOf(medicalStore),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList()
        )

        assertTrue(status is JoinStatus.ActiveStore)
        assertEquals("medical", (status as JoinStatus.ActiveStore).businessType)
    }

    @Test
    fun multiEntityOwnerRespectsPreferredRoleDashboard() {
        val ownerPhone = "777555444"
        val store = StoreEntity(
            id = "s_1",
            sectionId = "stores",
            name = "متجر النور",
            phone = ownerPhone,
            ownerId = ownerPhone,
            isActive = true,
            isApproved = true
        )
        val property = PropertyEntity(
            id = "prop_1",
            title = "عمارة الروضة",
            phone = ownerPhone,
            ownerId = ownerPhone,
            isActive = true,
            isApproved = true
        )
        val provider = ProviderEntity(
            id = "prov_1",
            name = "م. خالد",
            phone = ownerPhone,
            categoryId = "sub_1_2",
            subscriptionStatus = "APPROVED"
        )
        val categories = listOf(CategoryEntity(id = "sub_1_2", name = "كهرباء ومولدات"))

        val propStatus = useCase.determineStatus(
            joinPhone = ownerPhone,
            pendingProviders = emptyList(),
            providers = listOf(provider),
            stores = listOf(store),
            properties = listOf(property),
            categories = categories,
            notifications = emptyList(),
            preferredRole = "PROPERTY_DASHBOARD"
        )
        assertTrue(propStatus is JoinStatus.ActiveProperty)

        val techStatus = useCase.determineStatus(
            joinPhone = ownerPhone,
            pendingProviders = emptyList(),
            providers = listOf(provider),
            stores = listOf(store),
            properties = listOf(property),
            categories = categories,
            notifications = emptyList(),
            preferredRole = "TECHNICIAN_DASHBOARD"
        )
        assertTrue(techStatus is JoinStatus.ApprovedTechnician)
        assertEquals("كهرباء ومولدات", (techStatus as JoinStatus.ApprovedTechnician).categoryName)

        val storeStatus = useCase.determineStatus(
            joinPhone = ownerPhone,
            pendingProviders = emptyList(),
            providers = listOf(provider),
            stores = listOf(store),
            properties = listOf(property),
            categories = categories,
            notifications = emptyList(),
            preferredRole = "STORE_DASHBOARD"
        )
        assertTrue(storeStatus is JoinStatus.ActiveStore)
        assertEquals("stores", (storeStatus as JoinStatus.ActiveStore).businessType)
    }

    @Test
    fun medicalProviderFallbackMatchesPreferredMedicalRole() {
        val medProvider = ProviderEntity(
            id = "med_prov_1",
            providerType = "medical",
            name = "د. طارق السالم",
            phone = "771998877",
            profession = "استشاري باطنية وعيادة خاصة",
            subscriptionStatus = "APPROVED"
        )

        val status = useCase.determineStatus(
            joinPhone = "771998877",
            pendingProviders = emptyList(),
            providers = listOf(medProvider),
            stores = emptyList(),
            properties = emptyList(),
            categories = emptyList(),
            notifications = emptyList(),
            preferredRole = "MEDICAL_DASHBOARD"
        )

        assertTrue(status is JoinStatus.ApprovedTechnician)
        assertEquals("med_prov_1", (status as JoinStatus.ApprovedTechnician).provider.id)
    }

    @Test
    fun productItemEntitySupportsOfferDiscountOldPriceAndUnitFields() {
        val defaultProduct = ProductItemEntity(id = "p1", ownerId = "o1", title = "سخان شمسي", priceYer = 85000.0)
        assertFalse(defaultProduct.isOffer)
        assertEquals(0, defaultProduct.discountPercent)
        assertEquals(0.0, defaultProduct.oldPriceYer, 0.01)
        assertEquals("", defaultProduct.unit)

        val offerProduct = ProductItemEntity(
            id = "p2",
            ownerId = "o1",
            title = "لوح طاقة 550 واط",
            priceYer = 60000.0,
            isOffer = true,
            discountPercent = 20,
            oldPriceYer = 75000.0,
            unit = "قطعة"
        )
        assertTrue(offerProduct.isOffer)
        assertEquals(20, offerProduct.discountPercent)
        assertEquals(75000.0, offerProduct.oldPriceYer, 0.01)
        assertEquals("قطعة", offerProduct.unit)
    }

    @Test
    fun phoneNormalizationHandlesYemenFormatsUniformly() {
        val n1 = AppPreferenceHelper.normalizePhoneNumber("+967 777 123 456")
        val n2 = AppPreferenceHelper.normalizePhoneNumber("00967777123456")
        val n3 = AppPreferenceHelper.normalizePhoneNumber("777123456")
        assertEquals(n3, n1)
        assertEquals(n3, n2)
        assertEquals("777123456", n3)
    }
}
