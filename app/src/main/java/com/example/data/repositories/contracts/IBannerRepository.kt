package com.example.data.repositories.contracts

import com.example.data.BannerEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IBannerRepository {
    fun observeBanners(): Flow<AppResult<List<BannerEntity>>>
    suspend fun addBanner(banner: BannerEntity): AppResult<Unit>
    suspend fun updateBanner(banner: BannerEntity): AppResult<Unit>
    suspend fun deleteBanner(bannerId: String): AppResult<Unit>
}
