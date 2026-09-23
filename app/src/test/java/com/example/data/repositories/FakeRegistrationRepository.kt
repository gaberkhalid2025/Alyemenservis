package com.example.data.repositories

import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 🧪 FakeRegistrationRepository - مستودع تسجيل بديل مخصص لاختبارات الوحدات الخاطفة
 */
class FakeRegistrationRepository : IRegistrationRepository {
    private val fakeDatabase = mutableListOf<RegistrationEntity>()

    override suspend fun registerClient(client: RegistrationEntity.Client): Result<String> {
        fakeDatabase.add(client)
        return Result.success("fake_client_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerProvider(provider: RegistrationEntity.Provider): Result<String> {
        fakeDatabase.add(provider)
        return Result.success("fake_provider_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerStore(store: RegistrationEntity.Store): Result<String> {
        fakeDatabase.add(store)
        return Result.success("fake_store_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerRestaurant(restaurant: RegistrationEntity.Restaurant): Result<String> {
        fakeDatabase.add(restaurant)
        return Result.success("fake_restaurant_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerMedicalCenter(medical: RegistrationEntity.MedicalCenter): Result<String> {
        fakeDatabase.add(medical)
        return Result.success("fake_medical_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerProperty(property: RegistrationEntity.Property): Result<String> {
        fakeDatabase.add(property)
        return Result.success("fake_property_id_${System.currentTimeMillis()}")
    }

    override suspend fun registerJob(job: RegistrationEntity.Job): Result<String> {
        fakeDatabase.add(job)
        return Result.success("fake_job_id_${System.currentTimeMillis()}")
    }

    override fun getJoinStatusFlow(phoneNumber: String): Flow<JoinStatusEntity?> {
        return flowOf(JoinStatusEntity(requestId = "req_123", applicantName = "اختبار", status = "PENDING"))
    }
}
