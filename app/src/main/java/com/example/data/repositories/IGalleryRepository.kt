package com.example.data.repositories

import com.example.data.utils.AppResult
import com.example.domain.entities.GalleryAlbumEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IGalleryRepository {
    fun getOwnerGallery(ownerId: String): Flow<List<GalleryAlbumEntity>> = flowOf(emptyList())
    suspend fun uploadImageStringOrUri(uri: String): AppResult<String> = AppResult.success(uri)
}
