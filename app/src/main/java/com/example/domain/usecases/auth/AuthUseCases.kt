package com.example.domain.usecases.auth

import com.example.domain.entities.RegistrationEntity
import com.example.domain.entities.AuthUserEntity
import com.example.data.models.JoinRequestEntity
import com.example.data.ProviderEntity
import com.example.data.StoreEntity
import com.example.data.PropertyEntity
import com.example.utils.AppResult
import com.example.utils.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Mocks to allow compilation since these shouldn't be created as separate files yet
class AuthRepository {
    suspend fun login(phone: String, password: String): AppResult<AuthUserEntity> = AppResult.Error(AppError.UnknownError("Not implemented"))
    suspend fun register(name: String, phone: String, password: String, residence: String): AppResult<AuthUserEntity> = AppResult.Error(AppError.UnknownError("Not implemented"))
    suspend fun loginAsGuest(phone: String): AppResult<AuthUserEntity> = AppResult.Error(AppError.UnknownError("Not implemented"))
    suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun requestPasswordReset(phone: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun adminResetPassword(phone: String, newPassword: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun verifyAdminCredentials(username: String, password: String, ownerPass: String = ""): AppResult<AdminRole> = AppResult.Error(AppError.UnknownError("Not implemented"))
    fun getJoinStatus(phone: String): Flow<JoinStatusEntity?> = emptyFlow()
    fun isUserLoggedIn(): Boolean = false
    fun getCurrentUser(): AuthUserEntity? = null
}

class UserRepository {
    suspend fun findAccountByPhone(phone: String): AppResult<RestoreAccountMatch> = AppResult.Error(AppError.UnknownError("Not implemented"))
}

data class JoinStatusEntity(val status: String)

/**
 * 🎯 AuthUseCases
 * UseCases موحدة لمصادقة المستخدمين وإدارة الهوية
 */
class AuthUseCases(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    
    suspend fun login(phone: String, password: String): AppResult<AuthUserEntity> {
        return authRepository.login(phone, password)
    }
    
    suspend fun register(
        name: String,
        phone: String,
        password: String,
        residence: String = ""
    ): AppResult<AuthUserEntity> {
        return authRepository.register(name, phone, password, residence)
    }
    
    suspend fun loginAsGuest(phone: String): AppResult<AuthUserEntity> {
        return authRepository.loginAsGuest(phone)
    }
    
    suspend fun logout(): AppResult<Unit> {
        return authRepository.logout()
    }
    
    suspend fun restoreAccount(phone: String): AppResult<RestoreAccountMatch> {
        return userRepository.findAccountByPhone(phone)
    }
    
    suspend fun requestPasswordReset(phone: String): AppResult<Unit> {
        return authRepository.requestPasswordReset(phone)
    }
    
    suspend fun adminResetPassword(phone: String, newPassword: String): AppResult<Unit> {
        return authRepository.adminResetPassword(phone, newPassword)
    }
    
    suspend fun verifyAdminCredentials(username: String, password: String, ownerPass: String = ""): AppResult<AdminRole> {
        return authRepository.verifyAdminCredentials(username, password, ownerPass)
    }
    
    fun getJoinStatus(phone: String): Flow<JoinStatusEntity?> {
        return authRepository.getJoinStatus(phone)
    }
    
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
    
    fun getCurrentUser(): AuthUserEntity? {
        return authRepository.getCurrentUser()
    }
}

/**
 * نتيجة البحث عن حساب للاستعادة
 */
data class RestoreAccountMatch(
    val type: String, // "PROVIDER", "STORE", "PROPERTY", "CLIENT"
    val name: String,
    val provider: ProviderEntity? = null,
    val store: StoreEntity? = null,
    val property: PropertyEntity? = null,
    val savedPassword: String = ""
)

enum class AdminRole {
    OWNER, ADMIN, SUPERVISOR, GUEST
}
