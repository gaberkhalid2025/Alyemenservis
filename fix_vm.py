import re
import os

dir_path = "app/src/main/java/com/example/ui"

# Read original base MainViewModel from backup
with open("wam_backup_04_07/MainViewModel.kt") as f:
    text = f.read()

lines = text.split("\n")

# Make private val/var/fun internal
new_lines = []
for line in lines:
    if line.strip().startswith("private val "):
        line = line.replace("private val ", "internal val ")
    elif line.strip().startswith("private var "):
        line = line.replace("private var ", "internal var ")
    elif line.strip().startswith("private fun "):
        line = line.replace("private fun ", "internal fun ")
    new_lines.append(line)

# Search and replace signature of registerGuestUser
found_signature = False
for i, line in enumerate(new_lines):
    if "fun registerGuestUser(" in line and "extraParam" not in line and "password" not in line:
        new_lines[i] = "    fun registerGuestUser(context: android.content.Context, name: String, phone: String, residence: String, password: String? = null) {"
        found_signature = True
        print(f"Updated registerGuestUser signature at line {i+1}")
        break

if not found_signature:
    print("Warning: could not find registerGuestUser signature to update!")

# Search and replace mapOf in registerGuestUser body
full_text = "\n".join(new_lines)

target_mapOf = """                        val regUser = mapOf(
                            "id" to newUserId,
                            "name" to name,
                            "phone" to phone,
                            "residence" to residence,
                            "androidId" to androidId,
                            "timestamp" to System.currentTimeMillis()
                        )"""

replacement_mapOf = """                        val regUser = mutableMapOf(
                            "id" to newUserId,
                            "name" to name,
                            "phone" to phone,
                            "residence" to residence,
                            "androidId" to androidId,
                            "timestamp" to System.currentTimeMillis()
                        )
                        if (password != null) {
                            regUser["password"] = password
                        }"""

if target_mapOf in full_text:
    full_text = full_text.replace(target_mapOf, replacement_mapOf)
    print("Replaced regUser mapOf in registerGuestUser body.")
else:
    # Try with single spaces or flexible search if exact match fails
    print("Warning: target_mapOf exact match not found. Trying regex or manual replace...")
    # Standard replacement of mapOf inside registerGuestUser
    full_text = re.sub(
        r'val\s+regUser\s*=\s*mapOf\(\s*"id"\s*to\s*newUserId,\s*"name"\s*to\s*name,\s*"phone"\s*to\s*phone,\s*"residence"\s*to\s*residence,\s*"androidId"\s*to\s*androidId,\s*"timestamp"\s*to\s*System\.currentTimeMillis\(\)\s*\)',
        replacement_mapOf,
        full_text,
        flags=re.DOTALL
    )

# Find the last closing brace of MainViewModel class
# Class is defined as: class MainViewModel : ViewModel() { ... }
# Let us find the last closing brace in the file
new_lines_rebuild = full_text.split("\n")
class_end_idx = -1
for i in range(len(new_lines_rebuild) - 1, -1, -1):
    if new_lines_rebuild[i].strip() == "}":
        class_end_idx = i
        break

print(f"Last closing brace found at line {class_end_idx+1}")

additional_stateflows = """
    // Additional StateFlows for app compatibility
    internal val _stores = MutableStateFlow<List<StoreEntity>>(emptyList())
    val stores: StateFlow<List<StoreEntity>> = _stores.asStateFlow()

    internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
    val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()

    internal val _properties = MutableStateFlow<List<PropertyEntity>>(emptyList())
    val properties: StateFlow<List<PropertyEntity>> = _properties.asStateFlow()

    internal val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    internal val _triggerRestoreAccountDialog = MutableStateFlow(false)
    val triggerRestoreAccountDialog: StateFlow<Boolean> = _triggerRestoreAccountDialog.asStateFlow()

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    internal val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    internal val _selectedProvider = MutableStateFlow<ProviderEntity?>(null)
    val selectedProvider: StateFlow<ProviderEntity?> = _selectedProvider.asStateFlow()

    internal val _selectedStore = MutableStateFlow<StoreEntity?>(null)
    val selectedStore: StateFlow<StoreEntity?> = _selectedStore.asStateFlow()

    internal val _selectedProperty = MutableStateFlow<PropertyEntity?>(null)
    val selectedProperty: StateFlow<PropertyEntity?> = _selectedProperty.asStateFlow()

    internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("HOME"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

    internal val _activeVoiceCall = MutableStateFlow<Map<String, Any>?>(null)
    val activeVoiceCall: StateFlow<Map<String, Any>?> = _activeVoiceCall.asStateFlow()

    internal val _customProfileTabs = MutableStateFlow<List<CustomProfileTabEntity>>(emptyList())
    val customProfileTabs: StateFlow<List<CustomProfileTabEntity>> = _customProfileTabs.asStateFlow()

    internal val _passwordRecoveryWaitingPhone = MutableStateFlow("")
    val passwordRecoveryWaitingPhone: StateFlow<String> = _passwordRecoveryWaitingPhone.asStateFlow()

    fun setPasswordRecoveryWaitingPhone(phone: String) {
        _passwordRecoveryWaitingPhone.value = phone
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
    }

    fun updateUserFcmToken(userId: String, token: String) {
        if (userId.isNotEmpty() && userId != "guest") {
            db.collection("registered_users").document(userId).update("fcmToken", token)
                .addOnFailureListener {
                    db.collection("providers").document(userId).update("fcmToken", token)
                }
        }
    }

    fun triggerRestoreAccountDialog(show: Boolean) {
        _triggerRestoreAccountDialog.value = show
    }

    fun closeActiveChatChannel() {
        _activeChatChannel.value = null
    }

    fun clearUiError() {
        _uiErrorMessage.value = null
    }

    fun setUiError(message: String) {
        _uiErrorMessage.value = message
    }

    fun refreshData() {
        _isRefreshing.value = true
        _isRefreshing.value = false
    }

    fun retryConnection() {
        _isOnline.value = true
        refreshData()
    }

    fun setUserSessionDetails(context: android.content.Context, name: String, phone: String, residence: String) {
        _currentUserId.value = "user_" + phone.trim().replace(" ", "").replace("+", "")
        _currentUserName.value = name
        _currentUserPhone.value = phone
        _currentUserResidence.value = residence
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().apply {
            putString("user_id", _currentUserId.value)
            putString("user_name", name)
            putString("user_phone", phone)
            putString("user_residence", residence)
            apply()
        }
    }
"""

final_base = new_lines_rebuild[:class_end_idx] + [additional_stateflows] + ["}\n"]

with open(os.path.join(dir_path, "MainViewModel.kt"), "w") as f:
    f.write("\n".join(final_base))

print("Successfully wrote MainViewModel.kt with all newlines and additional StateFlows!")
