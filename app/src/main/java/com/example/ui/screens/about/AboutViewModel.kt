package com.example.ui.screens.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different UI States for the About App screen.
 */
sealed class AboutUiState {
    object Loading : AboutUiState()
    data class Success(val settings: AdminSettingsEntity, val isAdmin: Boolean) : AboutUiState()
    data class Editing(val settings: AdminSettingsEntity) : AboutUiState()
    data class Error(val message: String) : AboutUiState()
}

/**
 * ViewModel for managing the "About App" screen settings and layouts.
 */
class AboutViewModel(
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private var isEditingMode = false

    init {
        viewModelScope.launch {
            mainViewModel.settings.collectLatest { settings ->
                val adminRole = mainViewModel.adminRole.value
                val isAdmin = adminRole != "GUEST"
                if (isEditingMode) {
                    _uiState.value = AboutUiState.Editing(settings)
                } else {
                    _uiState.value = AboutUiState.Success(settings, isAdmin)
                }
            }
        }
    }

    /**
     * Toggles between standard view and editing view.
     */
    fun toggleEditingMode() {
        setEditingMode(!isEditingMode)
    }

    /**
     * Explicitly sets whether the editor is shown.
     */
    fun setEditingMode(editing: Boolean) {
        isEditingMode = editing
        val settings = mainViewModel.settings.value
        if (editing) {
            _uiState.value = AboutUiState.Editing(settings)
        } else {
            val adminRole = mainViewModel.adminRole.value
            val isAdmin = adminRole != "GUEST"
            _uiState.value = AboutUiState.Success(settings, isAdmin)
        }
    }

    /**
     * Moves an item up or down in the rendering order.
     */
    fun moveItem(index: Int, moveUp: Boolean) {
        val settings = mainViewModel.settings.value
        val list = settings.aboutLayoutOrder
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (moveUp && index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
        } else if (!moveUp && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
        }
        val newOrder = list.joinToString(",")
        mainViewModel.saveCustomSettingsState(settings.copy(aboutLayoutOrder = newOrder))
    }

    /**
     * Updates the custom description text.
     */
    fun updateCustomInfo(newInfo: String) {
        val settings = mainViewModel.settings.value
        mainViewModel.saveCustomSettingsState(settings.copy(aboutCustomInfo = newInfo))
        mainViewModel.triggerNotification("💾 تم تحديث وحفظ نص شاشة عن التطبيق!")
    }

    /**
     * Updates contact info (WhatsApp, Phone, Email).
     */
    fun updateContactInfo(whatsapp: String, phone: String, email: String) {
        val settings = mainViewModel.settings.value
        val updated = settings.copy(
            supportWhatsapp = whatsapp.trim(),
            supportPhone = phone.trim(),
            supportEmail = email.trim()
        )
        mainViewModel.saveCustomSettingsState(updated)
        mainViewModel.triggerNotification("💾 تم حفظ وتحديث أرقام ووسائل الدعم الفني بنجاح!")
    }

    /**
     * Updates social media links and URLs.
     */
    fun updateSocialLinks(
        telegram: String,
        twitter: String,
        facebook: String,
        website: String,
        instagram: String,
        youtube: String,
        downloadUrl: String
    ) {
        val settings = mainViewModel.settings.value
        val updated = settings.copy(
            telegramUrl = telegram.trim(),
            twitterUrl = twitter.trim(),
            facebookUrl = facebook.trim(),
            websiteUrl = website.trim(),
            instagramUrl = instagram.trim(),
            youtubeUrl = youtube.trim(),
            appDownloadUrl = downloadUrl.trim()
        )
        mainViewModel.saveCustomSettingsState(updated)
        mainViewModel.triggerNotification("💾 تم حفظ وتحديث روابط التواصل والموقع بنجاح!")
    }

    /**
     * Toggles visibility of specific social platforms.
     */
    fun toggleSocialVisibility(platform: String, hide: Boolean) {
        val settings = mainViewModel.settings.value
        val updated = when (platform.uppercase()) {
            "TELEGRAM" -> settings.copy(hideTelegram = hide)
            "TWITTER" -> settings.copy(hideTwitter = hide)
            "FACEBOOK" -> settings.copy(hideFacebook = hide)
            "WEBSITE" -> settings.copy(hideWebsite = hide)
            "INSTAGRAM" -> settings.copy(hideInstagram = hide)
            "YOUTUBE" -> settings.copy(hideYoutube = hide)
            else -> settings
        }
        mainViewModel.saveCustomSettingsState(updated)
    }

    /**
     * Updates app general identity info.
     */
    fun updateAppIdentity(appName: String, appVersion: String, bannerContent: String) {
        val settings = mainViewModel.settings.value
        val updated = settings.copy(
            appName = appName.trim(),
            appVersion = appVersion.trim(),
            bannerContent = bannerContent.trim()
        )
        mainViewModel.saveCustomSettingsState(updated)
        mainViewModel.triggerNotification("💾 تم تحديث وحفظ هوية التطبيق بنجاح!")
    }
}
