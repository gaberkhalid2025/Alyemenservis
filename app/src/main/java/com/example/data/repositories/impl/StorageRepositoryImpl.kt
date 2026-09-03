package com.example.data.repositories.impl

import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.repositories.contracts.IStorageRepository
import com.example.data.repositories.FirebaseOptimizationManager
import com.example.util.FirebaseStorageUploader
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import java.util.UUID

class StorageRepositoryImpl : IStorageRepository {

    override suspend fun compressAndUploadImage(imageUriOrBytes: ByteArray, folderName: String): AppResult<String> {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageUriOrBytes, 0, imageUriOrBytes.size)
            val compressedBase64 = if (bitmap != null) {
                FirebaseOptimizationManager.compressImageToWebP(bitmap)
            } else {
                Base64.encodeToString(imageUriOrBytes, Base64.DEFAULT)
            }
            
            val decodedBytes = Base64.decode(compressedBase64, Base64.DEFAULT)
            
            val uniqueId = UUID.randomUUID().toString().take(8)
            val storagePath = "$folderName/image_$uniqueId.webp"
            
            FirebaseStorageUploader.uploadBytesToStorage(
                bytes = decodedBytes,
                storagePath = storagePath,
                mimeType = "image/webp"
            )
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل ضغط ورفع الصورة"))
        }
    }

    override suspend fun uploadMultipleImages(imagesBytes: List<ByteArray>, folderName: String): AppResult<List<String>> {
        return try {
            val urls = mutableListOf<String>()
            for (bytes in imagesBytes) {
                val result = compressAndUploadImage(bytes, folderName)
                if (result.isSuccess) {
                    urls.add(result.getOrThrow())
                } else {
                    return Result.failure(result.exceptionOrNull() ?: Exception("خطأ أثناء رفع إحدى الصور"))
                }
            }
            Result.success(urls)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل رفع مجموعة الصور"))
        }
    }
}
