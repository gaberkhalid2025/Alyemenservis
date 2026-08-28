package com.example.ui.screens.register.forms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.MainViewModel
import com.example.util.FirebaseStorageUploader
import com.example.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 🏛️ FormUiState - الموحد لإدارة حالات جميع نماذج التسجيل بالنمط التصريحي المعماري
 */
sealed class FormUiState {
    object Idle : FormUiState()
    data class Loading(
        val progress: Float? = null,
        val stageMessage: String = "جاري المعالجة..."
    ) : FormUiState()

    data class Success(
        val requestId: String,
        val message: String
    ) : FormUiState()

    data class Error(
        val errorMessage: String,
        val fieldErrors: Map<String, String>? = null
    ) : FormUiState()
}

/**
 * 🧠 RegistrationFormViewModel - إدارة الحالات المستقلة والمشتركة لكافة نماذج التسجيل
 *
 * @property mainViewModel ViewModel الرئيسي للتطبيق
 */
class RegistrationFormViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {

    private val _formState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val formState: StateFlow<FormUiState> = _formState.asStateFlow()

    private val _isCompressingImages = MutableStateFlow(false)
    val isCompressingImages: StateFlow<Boolean> = _isCompressingImages.asStateFlow()

    /**
     * إعادة تعيين حالة النموذج لـ Idle لمنع ظهور رسائل الأخطاء القديمة عند إعادة الفتح
     */
    fun resetState() {
        _formState.value = FormUiState.Idle
        _isCompressingImages.value = false
    }

    /**
     * تحديث حالة النموذج يدوياً
     */
    fun setFormState(state: FormUiState) {
        _formState.value = state
    }

    /**
     * معالج الأخطاء المركزي الشامل (Global Error Handler)
     */
    fun handleException(throwable: Throwable, defaultMessage: String = "حدث خطأ غير متوقع أثناء معالجة النموذج") {
        Log.e("RegistrationFormViewModel", "Unhandled Exception caught", throwable)
        val readableMessage = when (throwable) {
            is IllegalArgumentException -> throwable.message ?: defaultMessage
            is java.net.UnknownHostException -> "تعذر الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت."
            else -> throwable.message ?: defaultMessage
        }
        _formState.value = FormUiState.Error(errorMessage = readableMessage)
    }

    /**
     * ضغط قائمة الصور في الخلفية باستخدام [Dispatchers.IO] دون تجميد واجهة المستخدم
     *
     * @param context سياق التطبيق
     * @param uris عناوين الصور الأصلية
     * @return قائمة بعناوين الملفات المضغوطة
     */
    suspend fun compressImagesInBackground(context: Context, uris: List<Uri>): List<Uri> {
        return withContext(Dispatchers.IO) {
            _isCompressingImages.value = true
            try {
                uris.map { uri ->
                    compressSingleUri(context, uri)
                }
            } catch (e: Exception) {
                Log.e("RegistrationFormVM", "Error compressing images", e)
                uris
            } finally {
                _isCompressingImages.value = false
            }
        }
    }

    private fun compressSingleUri(context: Context, uri: Uri): Uri {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uri
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return uri
            val tempFile = File(context.cacheDir, "comp_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            var quality = 85
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.toByteArray().size > 300 * 1024 && quality > 20) {
                out.reset()
                quality -= 15
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            val fos = FileOutputStream(tempFile)
            fos.write(out.toByteArray())
            fos.flush()
            fos.close()
            bitmap.recycle()
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e("RegistrationFormVM", "Failed single compression", e)
            uri
        }
    }

    /**
     * رفع الصور بالسحابة مع مسار مخصص (Path Generator) وتحديث التغذية الراجعة
     */
    suspend fun uploadImages(
        context: Context,
        imagesUris: List<Uri>,
        pathGenerator: (Int) -> String
    ): List<String> {
        if (imagesUris.isEmpty()) return emptyList()

        _formState.value = FormUiState.Loading(progress = 0.2f, stageMessage = "جاري تجهيز وضغط الصور... 20%")
        val compressedUris = compressImagesInBackground(context, imagesUris)

        val uploadedUrls = mutableListOf<String>()
        compressedUris.forEachIndexed { idx, uri ->
            val stepProgress = 0.2f + (((idx + 1).toFloat() / compressedUris.size) * 0.7f)
            val percent = (stepProgress * 100).toInt()
            _formState.value = FormUiState.Loading(
                progress = stepProgress,
                stageMessage = "جاري رفع الصور للسحابة (${idx + 1}/${compressedUris.size})... $percent%"
            )
            val res = FirebaseStorageUploader.uploadImageUri(context, uri, pathGenerator(idx))
            res.getOrNull()?.let { uploadedUrls.add(it) }
        }
        return uploadedUrls
    }

    /**
     * رفع الصور بالسحابة مع تحديث مراحل التقدم المتعددة (Multi-Stage Upload Progress)
     */
    suspend fun uploadImagesMultiStage(
        context: Context,
        imagesUris: List<Uri>,
        folderName: String
    ): List<String> {
        if (imagesUris.isEmpty()) return emptyList()

        _formState.value = FormUiState.Loading(progress = 0.2f, stageMessage = "جاري ضغط وتجهيز الصور... 20%")
        val compressedUris = compressImagesInBackground(context, imagesUris)

        val uploadedUrls = mutableListOf<String>()
        compressedUris.forEachIndexed { idx, uri ->
            val stepProgress = 0.2f + (((idx + 1).toFloat() / compressedUris.size) * 0.5f)
            val percent = (stepProgress * 100).toInt()
            _formState.value = FormUiState.Loading(
                progress = stepProgress,
                stageMessage = "جاري رفع الصور للسحابة ($idx+1/${compressedUris.size})... $percent%"
            )
            val res = FirebaseStorageUploader.uploadImageUri(context, uri, "$folderName/${System.currentTimeMillis()}_$idx.jpg")
            res.getOrNull()?.let { uploadedUrls.add(it) }
        }
        return uploadedUrls
    }
}
