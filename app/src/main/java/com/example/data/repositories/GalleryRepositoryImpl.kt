package com.example.data.repositories

import android.content.Context
import com.example.data.utils.AppResult
import com.example.domain.entities.GalleryAlbumEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class GalleryRepositoryImpl(private val context: Context? = null) : IGalleryRepository {
    override fun getOwnerGallery(ownerId: String): Flow<List<GalleryAlbumEntity>> = flowOf(emptyList())
    override suspend fun uploadImageStringOrUri(uri: String): AppResult<String> = AppResult.success(uri)
}
