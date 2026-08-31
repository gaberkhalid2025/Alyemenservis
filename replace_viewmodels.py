import os
import re

mapping = {
    # Auth
    'loginUserDirectly': 'authViewModel', 'initializeUserIdentity': 'authViewModel',
    'registerGuestUser': 'authViewModel', 'restoreGuestUser': 'authViewModel',
    'restoreUserAccountByPhoneAndPassword': 'authViewModel', 'setUserSessionDetails': 'authViewModel',
    'logout': 'authViewModel', 'currentUserId': 'authViewModel',
    'currentUserName': 'authViewModel', 'currentUserPhone': 'authViewModel',
    'currentUserResidence': 'authViewModel', 'uiErrorMessage': 'authViewModel',
    'clearUiError': 'authViewModel', 'isOnline': 'authViewModel',
    'userLatitude': 'authViewModel', 'userLongitude': 'authViewModel',
    'startLocationUpdates': 'authViewModel', 'updateUserLocation': 'authViewModel',
    
    # Registration
    'cancelOrResetJoinRequest': 'registrationViewModel', 'resetRegistrationState': 'registrationViewModel',
    'submitJoinForm': 'registrationViewModel', 'joinRequestPhone': 'registrationViewModel',
    'setJoinRequestPhone': 'registrationViewModel', 'setPasswordRecoveryWaitingPhone': 'registrationViewModel',
    'requestPasswordRecoveryGeneral': 'registrationViewModel', 'requestPasswordRecoveryForStore': 'registrationViewModel',
    'requestPasswordRecoveryForProperty': 'registrationViewModel',
    
    # Booking
    'bookings': 'bookingViewModel', 'createBooking': 'bookingViewModel',
    'createBookingDirectly': 'bookingViewModel', 'attemptCancelBookingImpl': 'bookingViewModel',
    'deleteBooking': 'bookingViewModel', 'deleteBookingImpl': 'bookingViewModel',
    'updateBooking': 'bookingViewModel', 'updateBookingImpl': 'bookingViewModel',
    'updateBookingStatus': 'bookingViewModel', 'addBooking': 'bookingViewModel',
    
    # InstantRequest
    'instantRequests': 'instantRequestViewModel', 'createInstantRequest': 'instantRequestViewModel',
    'requestOffers': 'instantRequestViewModel', 'submitOfferForRequest': 'instantRequestViewModel',
    'acceptRequestOffer': 'instantRequestViewModel', 'selectedRequestId': 'instantRequestViewModel',
    'selectedOfferId': 'instantRequestViewModel',
    
    # Chat
    'chatChannels': 'chatViewModel', 'activeChatChannel': 'chatViewModel',
    'openChatChannel': 'chatViewModel', 'openOrCreateChatChannel': 'chatViewModel',
    'getOrCreateChatChannel': 'chatViewModel', 'closeActiveChatChannel': 'chatViewModel',
    'sendMessageInChat': 'chatViewModel', 'replyToChatChannel': 'chatViewModel',
    'wipeOldChatChannels': 'chatViewModel', 'deleteChatChannel': 'chatViewModel',
    'blockChatChannel': 'chatViewModel', 'toggleBlockChatChannel': 'chatViewModel',
    'isGpsTrackingActive': 'chatViewModel',
    
    # Notification
    'notifications': 'notificationViewModel', 'addNotification': 'notificationViewModel',
    'clearNotification': 'notificationViewModel', 'deleteNotification': 'notificationViewModel',
    'triggerNotification': 'notificationViewModel', 'sendNotificationToApplicants': 'notificationViewModel',
    
    # Admin
    'adminRole': 'adminViewModel', 'authenticateAdmin': 'adminViewModel',
    'verifyAdminOrOwnerPassword': 'adminViewModel', 'adminResetAccountPassword': 'adminViewModel',
    'updateAdminSettings': 'adminViewModel', 'updateBackdoorSettings': 'adminViewModel',
    'dismissBackdoorDialog': 'adminViewModel', 'showBackdoorDialog': 'adminViewModel',
    'registerBackdoorInteraction': 'adminViewModel', 'settings': 'adminViewModel',
    'colorPalettes': 'adminViewModel', 'addColorPalette': 'adminViewModel',
    'deleteColorPalette': 'adminViewModel', 'customProfileTabs': 'adminViewModel',
    'saveCustomProfileTab': 'adminViewModel', 'toggleCustomProfileTab': 'adminViewModel',
    'deleteCustomProfileTab': 'adminViewModel', 'banners': 'adminViewModel',
    'addBanner': 'adminViewModel', 'addNewBanner': 'adminViewModel', 'deleteBanner': 'adminViewModel',
    'reorderBanners': 'adminViewModel', 'wipeAllMockAndTemporaryData': 'adminViewModel',
    'createSystemBackup': 'adminViewModel', 'restoreSystemFromBackup': 'adminViewModel',
    'wipeSelectedDatabaseData': 'adminViewModel', 'saveBackupToLocalStorage': 'adminViewModel',
    'saveCustomSettingsState': 'adminViewModel',
    
    # Provider
    'providers': 'providerViewModel', 'pendingProviders': 'providerViewModel',
    'filteredProviders': 'providerViewModel', 'selectedProvider': 'providerViewModel',
    'addNewProviderCustom': 'providerViewModel', 'removeProvider': 'providerViewModel',
    'deleteRegisteredUser': 'providerViewModel', 'toggleProviderBlock': 'providerViewModel',
    'toggleProviderSubscription': 'providerViewModel', 'pinProvider': 'providerViewModel',
    'recommendProvider': 'providerViewModel', 'setProviderChatDisabled': 'providerViewModel',
    'setProviderNotificationsDisabled': 'providerViewModel', 'setProviderPaymentRequired': 'providerViewModel',
    'extendProviderSubscription': 'providerViewModel', 'verifyProviderBadge': 'providerViewModel',
    'restoreProvider': 'providerViewModel', 'hardDeleteEntity': 'providerViewModel',
    'isProvidersLoading': 'providerViewModel', 'isProviderUser': 'providerViewModel',
    'approveTechnician': 'providerViewModel', 'rejectTechnician': 'providerViewModel',
    
    # Store
    'stores': 'storeViewModel', 'selectedStore': 'storeViewModel', 'addNewStore': 'storeViewModel',
    'deleteStore': 'storeViewModel', 'deleteStorePermanently': 'storeViewModel',
    'restoreStore': 'storeViewModel', 'toggleStoreBlocked': 'storeViewModel',
    'setStoreActive': 'storeViewModel', 'setStoreBlocked': 'storeViewModel',
    'setStoreChatDisabled': 'storeViewModel', 'setStoreNotificationsDisabled': 'storeViewModel',
    'setStorePinned': 'storeViewModel', 'setStoreRecommended': 'storeViewModel',
    'setStoreVerified': 'storeViewModel', 'setStoreVip': 'storeViewModel', 'approveStorePdf': 'storeViewModel',
    
    # Property
    'properties': 'propertyViewModel', 'selectedProperty': 'propertyViewModel',
    'deleteProperty': 'propertyViewModel', 'deletePropertyPermanently': 'propertyViewModel',
    'restoreProperty': 'propertyViewModel', 'togglePropertyBlocked': 'propertyViewModel',
    'setPropertyActive': 'propertyViewModel', 'setPropertyBlocked': 'propertyViewModel',
    'setPropertyChatDisabled': 'propertyViewModel', 'setPropertyNotificationsDisabled': 'propertyViewModel',
    'setPropertyPinned': 'propertyViewModel', 'setPropertyRecommended': 'propertyViewModel',
    'setPropertyVerified': 'propertyViewModel', 'setPropertyVip': 'propertyViewModel',
    'approvePropertyPdf': 'propertyViewModel',
    
    # Job
    'jobs': 'jobViewModel', 'jobApplications': 'jobViewModel', 'saveJob': 'jobViewModel',
    'deleteJob': 'jobViewModel', 'deleteJobPermanently': 'jobViewModel', 'restoreJob': 'jobViewModel',
    'setJobApproved': 'jobViewModel', 'setJobBlocked': 'jobViewModel', 'setJobChatDisabled': 'jobViewModel',
    'setJobPinned': 'jobViewModel', 'setJobVip': 'jobViewModel', 'exportJobApplicantsCsv': 'jobViewModel',
    'acceptJobApplication': 'jobViewModel', 'rejectJobApplication': 'jobViewModel',
    'deleteJobApplication': 'jobViewModel', 'updateJobApplicationStatus': 'jobViewModel',
    
    # Settings (Categories, Cities)
    'categories': 'settingsViewModel', 'cities': 'settingsViewModel',
    'addNewCategory': 'settingsViewModel', 'editCategory': 'settingsViewModel',
    'deleteCategory': 'settingsViewModel', 'reorderCategories': 'settingsViewModel',
    'selectCategory': 'settingsViewModel', 'selectedCategoryId': 'settingsViewModel',
    'addNewCity': 'settingsViewModel', 'updateCity': 'settingsViewModel', 'removeCity': 'settingsViewModel'
}

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if "MainViewModel" not in content and "viewModel." not in content:
        return

    used_vms = set()
    
    # Replace viewModel.xxx() and viewModel.xxx
    for method, vm in mapping.items():
        if f"viewModel.{method}" in content:
            content = content.replace(f"viewModel.{method}", f"{vm}.{method}")
            used_vms.add(vm)
            
    # Add imports if missing
    imports_to_add = set()
    for vm in used_vms:
        imports_to_add.add(f"import com.example.viewmodels.{vm[0].upper() + vm[1:]}")
        imports_to_add.add("import androidx.hilt.navigation.compose.hiltViewModel")
        
    for imp in imports_to_add:
        if imp not in content:
            # Add after standard imports
            content = re.sub(r'(import androidx.compose.*)', rf'\1\n{imp}', content, count=1)
            
    # Update signatures (very basic: look for `viewModel: MainViewModel` and replace it)
    if used_vms:
        vm_params = ",\n    ".join([f"{vm}: {vm[0].upper() + vm[1:]} = hiltViewModel()" for vm in used_vms])
        content = re.sub(r'viewModel:\s*MainViewModel,?', vm_params + ',', content)
        content = re.sub(r'viewModel\s*=\s*viewModel,?', '', content)
        
    with open(filepath, 'w') as f:
        f.write(content)

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            process_file(os.path.join(root, file))

