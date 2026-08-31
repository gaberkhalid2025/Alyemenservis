package com.example.data.repositories.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.repositories.contracts.IColorThemeRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class ColorThemeRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IColorThemeRepository {

    private val _themeFlow = MutableStateFlow("light")

    override fun getThemePreference(): Flow<String> {
        return _themeFlow.asStateFlow()
    }

    override suspend fun setThemePreference(theme: String): AppResult<Unit> {
        _themeFlow.value = theme
        return Result.success(Unit)
    }

    override suspend fun syncThemeWithServer(userId: String, theme: String): AppResult<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("themePreference", theme)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل مزامنة الثيم مع السيرفر"))
        }
    }

    override suspend fun fetchThemeFromServer(userId: String): AppResult<String> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val theme = document.getString("themePreference") ?: "light"
            _themeFlow.value = theme
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل جلب الثيم من السيرفر"))
        }
    }
}
