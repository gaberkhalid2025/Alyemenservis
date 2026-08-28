package com.example.ui.screens.register.forms

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.util.FirebaseStorageUploader
import com.example.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🧠 RegistrationFormViewModel - إدارة الحالات المستقلة والمشتركة لكافة نماذج التسجيل
 */
class RegistrationFormViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun setSubmitting(submitting: Boolean) {
        _isSubmitting.value = submitting
    }

    fun setUploadProgress(progress: Float) {
        _uploadProgress.value = progress
    }

    fun setStatusMessage(msg: String?) {
        _statusMessage.value = msg
    }

    suspend fun uploadImages(
        context: Context,
        imagesUris: List<Uri>,
        pathBuilder: (Int) -> String
    ): List<String> {
        val uploadedUrls = mutableListOf<String>()
        imagesUris.forEachIndexed { idx, uri ->
            _uploadProgress.value = (idx + 1).toFloat() / imagesUris.size
            val res = FirebaseStorageUploader.uploadImageUri(context, uri, pathBuilder(idx))
            res.getOrNull()?.let { uploadedUrls.add(it) }
        }
        return uploadedUrls
    }
}
