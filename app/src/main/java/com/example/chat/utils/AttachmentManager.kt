package com.example.chat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.media.ExifInterface
import com.example.chat.domain.MessageType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

/**
 * 📎 AttachmentMetadata
 * Value class summarizing extracted file attributes.
 */
data class AttachmentMetadata(
    val file: File,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val messageType: MessageType,
    val thumbnailFile: File? = null,
    val durationMillis: Long? = null,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * 🛠️ AttachmentManager
 * Multi-format attachment processing pipeline for Chat.
 * Handles compression, 200x200 thumbnail generation, EXIF auto-rotation, and metadata extraction.
 */
class AttachmentManager(private val context: Context) {

    private val TAG = "AttachmentManager"
    private val attachmentsDir = File(context.cacheDir, "chat_attachments").apply { if (!exists()) mkdirs() }

    /**
     * Processes any incoming content Uri (Image, Video, Document) into a local cached File with metadata.
     */
    fun processAttachment(uri: Uri): AttachmentMetadata? {
        return try {
            val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
            val mimeType = context.contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)
            val messageType = resolveMessageType(mimeType, fileName)

            // Copy content into local cache
            val localFile = File(attachmentsDir, "attach_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            var thumbnailFile: File? = null
            var durationMs: Long? = null
            var width = 0
            var height = 0

            when (messageType) {
                MessageType.IMAGE -> {
                    val result = generateImageThumbnail(localFile)
                    thumbnailFile = result.first
                    width = result.second
                    height = result.third
                }
                MessageType.VIDEO -> {
                    val result = generateVideoThumbnail(localFile)
                    thumbnailFile = result.first
                    durationMs = result.second
                    width = result.third
                    height = result.fourth
                }
                else -> {
                    // Documents, Audio, Contacts
                }
            }

            AttachmentMetadata(
                file = localFile,
                fileName = fileName,
                fileSize = localFile.length(),
                mimeType = mimeType,
                messageType = messageType,
                thumbnailFile = thumbnailFile,
                durationMillis = durationMs,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process attachment for URI: $uri", e)
            null
        }
    }

    /**
     * Generates a 200x200 thumbnail for an image, taking care of EXIF orientation.
     */
    private fun generateImageThumbnail(imageFile: File): Triple<File?, Int, Int> {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            val origWidth = options.outWidth
            val origHeight = options.outHeight

            // Decode scaled bitmap
            val sampleSize = calculateInSampleSize(options, 200, 200)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions) ?: return Triple(null, origWidth, origHeight)

            // Correct EXIF orientation
            val rotatedBitmap = rotateBitmapIfRequired(imageFile.absolutePath, decodedBitmap)

            // Scale precisely to 200x200 maintaining aspect ratio
            val thumbBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 200, 200, true)

            val thumbFile = File(attachmentsDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbFile).use { fos ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                fos.flush()
            }

            Triple(thumbFile, origWidth, origHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image thumbnail", e)
            Triple(null, 0, 0)
        }
    }

    /**
     * Generates a 200x200 thumbnail for a video file using MediaMetadataRetriever.
     */
    private fun generateVideoThumbnail(videoFile: File): Tuple4<File?, Long?, Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull()

            var thumbFile: File? = null
            var width = 0
            var height = 0

            if (frame != null) {
                width = frame.width
                height = frame.height
                val scaledFrame = Bitmap.createScaledBitmap(frame, 200, 200, true)
                thumbFile = File(attachmentsDir, "vthumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { fos ->
                    scaledFrame.compress(Bitmap.CompressFormat.JPEG, 75, fos)
                    fos.flush()
                }
            }

            Tuple4(thumbFile, durationMs, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video frame", e)
            Tuple4(null, null, 0, 0)
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun rotateBitmapIfRequired(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
    }

    private fun resolveMessageType(mimeType: String, fileName: String): MessageType {
        val lowerMime = mimeType.lowercase()
        val lowerName = fileName.lowercase()
        return when {
            lowerMime.startsWith("image/") -> MessageType.IMAGE
            lowerMime.startsWith("video/") -> MessageType.VIDEO
            lowerMime.startsWith("audio/") -> MessageType.AUDIO
            lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> MessageType.PDF
            lowerMime.contains("vcard") || lowerName.endsWith(".vcf") -> MessageType.CONTACT
            else -> MessageType.DOCUMENT
        }
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }
    }
}

/**
 * 📦 Helper tuple for 4 elements
 */
data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
