package com.example.data.repositories.contracts

import com.example.data.UserEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    fun clearListeners()
    suspend fun loginWithPhone(phone: String, pinOrPass: String): AppResult<UserEntity>
    suspend fun saveOrUpdateUser(user: UserEntity, passwordRaw: String = ""): AppResult<UserEntity>
    suspend fun getUserById(userId: String): AppResult<UserEntity?>
    suspend fun getUserByPhone(phone: String): AppResult<UserEntity?>
    fun observeUser(userId: String): Flow<UserEntity?>
    suspend fun updateFcmToken(userId: String, phone: String, token: String, role: String = "CLIENT"): AppResult<Unit>
    suspend fun resetPassword(phone: String, newPasswordRaw: String): AppResult<Unit>
}
