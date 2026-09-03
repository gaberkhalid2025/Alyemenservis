import re
import os

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Helper to extract functions
def extract_function(file_content, func_name):
    pattern = r'(?:override\s+|private\s+|internal\s+|suspend\s+)*fun\s+' + func_name + r'\b'
    matches = list(re.finditer(pattern, file_content))
    if not matches:
        return []
    results = []
    for match in matches:
        start_pos = match.start()
        brace_match = re.search(r'\{', file_content[start_pos:])
        if not brace_match:
            eq_match = re.search(r'=', file_content[start_pos:])
            if eq_match:
                line_end = file_content.find('\n', start_pos + eq_match.start())
                results.append(file_content[start_pos:line_end])
            continue
        
        brace_start = start_pos + brace_match.start()
        count = 1
        pos = brace_start + 1
        while count > 0 and pos < len(file_content):
            char = file_content[pos]
            if char == '{':
                count += 1
            elif char == '}':
                count -= 1
            pos += 1
        results.append(file_content[start_pos:pos])
    return results

# Helper to extract stateflows
def extract_stateflow(file_content, name):
    backing_pat = r'(?:internal\s+|private\s+|protected\s+)?val\s+_' + name + r'\s*=\s*(?:[^\n]+)'
    public_pat = r'(?:internal\s+|private\s+|protected\s+)?val\s+' + name + r'\s*:\s*(?:[^\n]+)'
    
    backing_match = re.search(backing_pat, file_content)
    public_match = re.search(public_pat, file_content)
    
    res = []
    if backing_match:
        res.append(backing_match.group(0))
    if public_match:
        res.append(public_match.group(0))
    return res

admin_funcs = [
    "approveRequest", "rejectRequest", "approveTechnician", "rejectTechnician", "loadPendingTechnicians",
    "approvePendingProvider", "rejectPendingProvider", "approveRegisteredUser", "toggleBlockRegisteredUser",
    "deleteRegisteredUser", "saveStore", "deleteStore", "restoreStore", "deleteStorePermanently",
    "setStoreActive", "setStorePinned", "setStoreVip", "setStoreVerified", "setStoreRecommended",
    "setStoreBlocked", "setStoreChatDisabled", "setStoreNotificationsDisabled", "setStorePaymentEnabled",
    "toggleStoreBlocked", "toggleStoreActive", "toggleStorePinned", "toggleStoreChatDisabled", "approveStorePdf",
    "saveProperty", "deleteProperty", "restoreProperty", "deletePropertyPermanently", "setPropertyActive",
    "setPropertyPinned", "setPropertyVip", "setPropertyVerified", "setPropertyRecommended", "setPropertyBlocked",
    "setPropertyChatDisabled", "setPropertyNotificationsDisabled", "setPropertyPaymentEnabled", "togglePropertyBlocked",
    "approvePropertyPdf", "saveJob", "deleteJob", "restoreJob", "deleteJobPermanently", "setJobApproved",
    "setJobBlocked", "setJobPinned", "setJobVip", "setJobChatDisabled", "submitJobApplication",
    "updateJobApplicationStatus", "acceptJobApplication", "rejectJobApplication", "deleteJobApplication",
    "submitReport", "deleteReport", "sendReport", "addCoupon", "saveCoupon", "deleteCoupon",
    "saveInternalWallet", "performWalletTransaction", "addPaymentWallet", "updatePaymentWallet",
    "deletePaymentWallet", "togglePaymentWalletVisibility", "createPayment", "confirmPayment", "verifyPayment",
    "refundPayment", "saveProduct", "deleteProduct", "updateProductPrice", "saveOffer", "deleteOffer",
    "toggleOfferStatus", "listenToOffersForEntity", "listenToProductsForStore", "saveCustomProfileTab",
    "deleteCustomProfileTab", "toggleCustomProfileTab", "addNewCategory", "editCategory", "deleteCategory",
    "togglePinCategory", "mergeCategories", "saveCategoryEntity", "addSubCategory", "convertCategoryType",
    "reorderCategories", "addNewCity", "updateCity", "removeCity", "removeProvider", "removeProviderPermanently",
    "restoreProvider", "pinProvider", "recommendProvider", "verifyProviderBadge", "toggleProviderSubscription",
    "setProviderChatDisabled", "setProviderNotificationsDisabled", "setProviderPaymentRequired",
    "extendProviderSubscription", "toggleProviderBlock", "toggleProviderStatus", "toggleProviderPin",
    "toggleProviderVerification", "toggleProviderRecommendation", "updateProviderEntity",
    "editProviderPhoneAndCategory", "addNewProvider", "addNewProviderCustom", "addNewBanner", "addBanner",
    "deleteBanner", "reorderBanners", "placeOrder", "updateOrderStatus", "deleteOrder", "deleteAllOrders",
    "addRating", "addRatingReply", "deleteRating", "approveRating", "submitRating", "recalculateTargetRating",
    "logAdminActivity", "logCall", "checkAndGetDuplicateAccountType", "updateProviderPortfolio",
    "addPortfolioImage", "removePortfolioImage", "clearPortfolio", "redirectBookingToEntity", "unbanEntity",
    "restoreEntity", "hardDeleteEntity", "sendNotificationToApplicants", "exportJobApplicantsCsv"
]

settings_funcs = [
    "loadCardSettings", "updateCardSettings", "updateTheme", "saveCustomSettingsState", "updateBackdoorSettings",
    "updateAdminSettings", "initColorSync", "updateCloudColorScheme", "updatePersonalColors", "triggerManualSync",
    "resolveConflict", "getCurrentTimestampString", "addNewSyncLog", "toggleChatParticipant", "isChatBlockedFor",
    "canParticipateInChat", "startVoiceCall", "endVoiceCall", "exportComplaintsToCSV", "exportComplaintsToPDF",
    "exportPerformanceReportToPDF", "createSystemBackup", "restoreSystemFromBackup", "exportSelectedCollectionsAsJson",
    "saveBackupToLocalStorage", "setSecondaryFirebaseConfig", "saveCustomPermissionsMatrixToFirestore",
    "addColorPalette", "updateColorPalette", "deleteColorPalette", "resetAccountPassword", "requestAdminPasswordReset",
    "requestPasswordReset", "approvePasswordReset", "adminResetAccountPassword", "requestPasswordRecoveryForStore",
    "requestPasswordRecoveryForProperty", "requestPasswordRecoveryGeneral", "wipeAllDatabaseData",
    "wipeSelectedDatabaseData", "wipeAllMockAndTemporaryData", "autoCleanupData", "scheduleAutoCleanup"
]

instant_funcs = [
    "createInstantRequest", "submitOfferForRequest", "acceptRequestOffer", "completeInstantRequest", "cancelInstantRequest"
]

admin_stateflows = [
    "pendingProviders", "pendingTechnicians", "registeredUsersList", "registeredUsersCount",
    "reports", "activityLogs", "callsLog", "coupons", "internalWallets", "walletTransactions",
    "paymentWallets", "payments", "orders", "ratings", "customProfileTabs",
    "stores", "products", "properties", "jobs", "jobApplications"
]

settings_stateflows = [
    "settings", "cardSettings", "colorScheme", "personalColors", "colorSyncStatus", "colorSyncLogs",
    "pendingConflictScheme", "blockedChatParticipants", "activeVoiceCall"
]

settings_extra_vars = [
    "private var colorSchemeListener: com.google.firebase.firestore.ListenerRegistration? = null",
    "private var userColorsListener: com.google.firebase.firestore.ListenerRegistration? = null"
]

instant_stateflows = [
    "instantRequests", "requestOffers", "offers"
]

card_settings_code = extract_function(content, "CardSettings")
if not card_settings_code:
    match = re.search(r'data class CardSettings\b', content)
    if match:
        start_pos = match.start()
        brace_start = content.find('{', start_pos)
        count = 1
        pos = brace_start + 1
        while count > 0 and pos < len(content):
            char = content[pos]
            if char == '{':
                count += 1
            elif char == '}':
                count -= 1
            pos += 1
        card_settings_code = [content[start_pos:pos]]
else:
    card_settings_code = [card_settings_code[0]]

chat_part_code = []
match = re.search(r'enum class ChatParticipantType\b', content)
if match:
    start_pos = match.start()
    brace_start = content.find('{', start_pos)
    count = 1
    pos = brace_start + 1
    while count > 0 and pos < len(content):
        char = content[pos]
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1
        pos += 1
    chat_part_code = [content[start_pos:pos]]

def safe_replace(code, word, replacement):
    pattern = r'(?<!val\s)(?<!var\s)(?<!fun\s)(?<!class\s)(?<!import\s)(?<!package\s)(?<!override\s)\b' + re.escape(word) + r'\b'
    
    def repl_func(match):
        start_idx = match.start()
        line_start = code.rfind('\n', 0, start_idx) + 1
        preceding_line = code[line_start:start_idx]
        if preceding_line.count('"') % 2 == 1:
            return match.group(0)
        return replacement
        
    return re.sub(pattern, repl_func, code)

def apply_replacements(code_body, repls):
    for word, repl in repls.items():
        code_body = safe_replace(code_body, word, repl)
    return code_body

shared_repls = {
    "addNotification": "mainViewModel.addNotification",
    "triggerNotification": "mainViewModel.triggerNotification",
    "_categories": "mainViewModel.homeViewModel._categories",
    "categories": "mainViewModel.homeViewModel.categories",
    "_providers": "mainViewModel.homeViewModel._providers",
    "providers": "mainViewModel.homeViewModel.providers",
    "_bookings": "mainViewModel.bookingViewModel._bookings",
    "bookings": "mainViewModel.bookingViewModel.bookings",
    "_cities": "mainViewModel._cities",
    "cities": "mainViewModel.cities",
    "_favoriteIds": "mainViewModel.authViewModel._favoriteIds",
    "favoriteIds": "mainViewModel.authViewModel.favoriteIds",
    "_deletedProviders": "mainViewModel._deletedProviders",
    "deletedProviders": "mainViewModel.deletedProviders",
    "_isProvidersLoading": "mainViewModel._isProvidersLoading",
    "isProvidersLoading": "mainViewModel.isProvidersLoading",
    "_isChatChannelsLoading": "mainViewModel._isChatChannelsLoading",
    "isChatChannelsLoading": "mainViewModel.isChatChannelsLoading",
    "_currentUserId": "mainViewModel.authViewModel._currentUserId",
    "currentUserId": "mainViewModel.authViewModel.currentUserId",
    "_currentUserName": "mainViewModel.authViewModel._currentUserName",
    "currentUserName": "mainViewModel.authViewModel.currentUserName",
    "_currentUserPhone": "mainViewModel.authViewModel._currentUserPhone",
    "currentUserPhone": "mainViewModel.authViewModel.currentUserPhone",
    "_currentUserResidence": "mainViewModel.authViewModel._currentUserResidence",
    "currentUserResidence": "mainViewModel.authViewModel.currentUserResidence",
    "_adminRole": "mainViewModel.authViewModel._adminRole",
    "adminRole": "mainViewModel.authViewModel.adminRole",
    "calculateDistance": "mainViewModel.calculateDistance",
    "getDistanceString": "mainViewModel.getDistanceString",
    "_userLatitude": "mainViewModel._userLatitude",
    "_userLongitude": "mainViewModel._userLongitude",
    "getProviderCoordinates": "mainViewModel.getProviderCoordinates"
}

admin_repls = shared_repls.copy()
admin_repls.update({
    "_settings": "mainViewModel.settingsViewModel._settings",
    "settings": "mainViewModel.settingsViewModel.settings",
    "_instantRequests": "mainViewModel.instantRequestViewModel._instantRequests",
    "instantRequests": "mainViewModel.instantRequestViewModel.instantRequests",
    "_requestOffers": "mainViewModel.instantRequestViewModel._requestOffers",
    "requestOffers": "mainViewModel.instantRequestViewModel.requestOffers"
})

settings_repls = shared_repls.copy()
settings_repls.update({
    "_pendingProviders": "mainViewModel.adminViewModel._pendingProviders",
    "pendingProviders": "mainViewModel.adminViewModel.pendingProviders",
    "_pendingTechnicians": "mainViewModel.adminViewModel._pendingTechnicians",
    "pendingTechnicians": "mainViewModel.adminViewModel.pendingTechnicians",
    "_registeredUsersList": "mainViewModel.adminViewModel._registeredUsersList",
    "registeredUsersList": "mainViewModel.adminViewModel.registeredUsersList",
    "_reports": "mainViewModel.adminViewModel._reports",
    "reports": "mainViewModel.adminViewModel.reports",
    "_activityLogs": "mainViewModel.adminViewModel._activityLogs",
    "activityLogs": "mainViewModel.adminViewModel.activityLogs",
    "_callsLog": "mainViewModel.adminViewModel._callsLog",
    "callsLog": "mainViewModel.adminViewModel.callsLog",
    "_coupons": "mainViewModel.adminViewModel._coupons",
    "coupons": "mainViewModel.adminViewModel.coupons",
    "_internalWallets": "mainViewModel.adminViewModel._internalWallets",
    "internalWallets": "mainViewModel.adminViewModel.internalWallets",
    "_walletTransactions": "mainViewModel.adminViewModel._walletTransactions",
    "walletTransactions": "mainViewModel.adminViewModel.walletTransactions",
    "_paymentWallets": "mainViewModel.adminViewModel._paymentWallets",
    "paymentWallets": "mainViewModel.adminViewModel.paymentWallets",
    "_payments": "mainViewModel.adminViewModel._payments",
    "payments": "mainViewModel.adminViewModel.payments",
    "_stores": "mainViewModel.adminViewModel._stores",
    "stores": "mainViewModel.adminViewModel.stores",
    "_products": "mainViewModel.adminViewModel._products",
    "products": "mainViewModel.adminViewModel.products",
    "_properties": "mainViewModel.adminViewModel._properties",
    "properties": "mainViewModel.adminViewModel.properties",
    "_jobs": "mainViewModel.adminViewModel._jobs",
    "jobs": "mainViewModel.adminViewModel.jobs",
    "_jobApplications": "mainViewModel.adminViewModel._jobApplications",
    "jobApplications": "mainViewModel.adminViewModel.jobApplications"
})

instant_repls = shared_repls.copy()
instant_repls.update({
    "_settings": "mainViewModel.settingsViewModel._settings",
    "settings": "mainViewModel.settingsViewModel.settings"
})

# Build AdminViewModel.kt content
admin_code = [
    "package com.example.ui.viewmodels",
    "",
    "import android.content.Context",
    "import androidx.lifecycle.viewModelScope",
    "import com.example.data.*",
    "import com.example.data.models.*",
    "import com.example.utils.*",
    "import kotlinx.coroutines.flow.*",
    "import kotlinx.coroutines.launch",
    "import java.util.UUID",
    "import com.google.firebase.firestore.FirebaseFirestore",
    "import com.google.firebase.firestore.ListenerRegistration",
    "import com.google.firebase.firestore.SetOptions",
    "",
    "class AdminViewModel : BaseViewModel() {",
    "    lateinit var mainViewModel: MainViewModel",
    ""
]

for sf in admin_stateflows:
    lines = extract_stateflow(content, sf)
    for line in lines:
        admin_code.append("    " + line)
admin_code.append("")

for f in admin_funcs:
    funcs = extract_function(content, f)
    for code in funcs:
        admin_code.append(apply_replacements(code, admin_repls))
        admin_code.append("")

admin_code.append("}")

with open('app/src/main/java/com/example/ui/viewmodels/AdminViewModel.kt', 'w', encoding='utf-8') as f:
    f.write("\n".join(admin_code))


# Build SettingsViewModel.kt content
settings_code = [
    "package com.example.ui.viewmodels",
    "",
    "import android.content.Context",
    "import androidx.lifecycle.viewModelScope",
    "import com.example.data.*",
    "import com.example.data.models.*",
    "import com.example.utils.*",
    "import kotlinx.coroutines.flow.*",
    "import kotlinx.coroutines.launch",
    "import java.util.UUID",
    "import com.google.firebase.firestore.FirebaseFirestore",
    "import com.google.firebase.firestore.ListenerRegistration",
    "import com.google.firebase.firestore.SetOptions",
    "",
    "class SettingsViewModel : BaseViewModel() {",
    "    lateinit var mainViewModel: MainViewModel",
    ""
]

for code in card_settings_code:
    settings_code.append(code)
    settings_code.append("")

for code in chat_part_code:
    settings_code.append(code)
    settings_code.append("")

for sf in settings_stateflows:
    lines = extract_stateflow(content, sf)
    for line in lines:
        settings_code.append("    " + line)
for line in settings_extra_vars:
    settings_code.append("    " + line)
settings_code.append("")

for f in settings_funcs:
    funcs = extract_function(content, f)
    for code in funcs:
        settings_code.append(apply_replacements(code, settings_repls))
        settings_code.append("")

settings_code.append("}")

with open('app/src/main/java/com/example/ui/viewmodels/SettingsViewModel.kt', 'w', encoding='utf-8') as f:
    f.write("\n".join(settings_code))


# Build InstantRequestViewModel.kt content
instant_code = [
    "package com.example.ui.viewmodels",
    "",
    "import android.content.Context",
    "import androidx.lifecycle.viewModelScope",
    "import com.example.data.*",
    "import com.example.data.models.*",
    "import com.example.utils.*",
    "import kotlinx.coroutines.flow.*",
    "import kotlinx.coroutines.launch",
    "import java.util.UUID",
    "import com.google.firebase.firestore.FirebaseFirestore",
    "import com.google.firebase.firestore.ListenerRegistration",
    "import com.google.firebase.firestore.SetOptions",
    "",
    "class InstantRequestViewModel : BaseViewModel() {",
    "    lateinit var mainViewModel: MainViewModel",
    ""
]

for sf in instant_stateflows:
    lines = extract_stateflow(content, sf)
    for line in lines:
        instant_code.append("    " + line)
instant_code.append("")

for f in instant_funcs:
    funcs = extract_function(content, f)
    for code in funcs:
        instant_code.append(apply_replacements(code, instant_repls))
        instant_code.append("")

instant_code.append("}")

with open('app/src/main/java/com/example/ui/viewmodels/InstantRequestViewModel.kt', 'w', encoding='utf-8') as f:
    f.write("\n".join(instant_code))

print("Regenerated 3 viewmodels with moved stores, products, properties, jobs, and jobApplications!")
