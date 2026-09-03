package com.example.data.repositories.contracts

import com.example.data.utils.AppResult

interface IStorageRepository {
    suspend fun compressAndUploadImage(imageUriOrBytes: ByteArray, folderName: String): AppResult<String>
    suspend fun uploadMultipleImages(imagesBytes: List<ByteArray>, folderName: String): AppResult<List<String>>
}
