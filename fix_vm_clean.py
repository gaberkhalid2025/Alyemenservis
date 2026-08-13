with open("app/src/main/java/com/example/ui/MainViewModel.kt") as f:
    content = f.read()

# Replace declarations around 2400-2440
old_block = """    internal val _triggerRestoreAccountDialog = MutableStateFlow(false)
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
    val activeVoiceCall: StateFlow<Map<String, Any>?> = _activeVoiceCall.asStateFlow()"""

new_block = """    val triggerRestoreAccountDialog = MutableStateFlow(false)

    internal val _activeChatChannel = MutableStateFlow<ChatChannelEntity?>(null)
    val activeChatChannel: StateFlow<ChatChannelEntity?> = _activeChatChannel.asStateFlow()

    internal val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    internal val _uiErrorMessage = MutableStateFlow<String?>(null)
    val uiErrorMessage: StateFlow<String?> = _uiErrorMessage.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    internal val _selectedProvider = MutableStateFlow<ProviderEntity?>(null)
    var selectedProvider: ProviderEntity?
        get() = _selectedProvider.value
        set(v) { _selectedProvider.value = v }
    val selectedProviderFlow: StateFlow<ProviderEntity?> = _selectedProvider.asStateFlow()

    internal val _selectedStore = MutableStateFlow<StoreEntity?>(null)
    var selectedStore: StoreEntity?
        get() = _selectedStore.value
        set(v) { _selectedStore.value = v }
    val selectedStoreFlow: StateFlow<StoreEntity?> = _selectedStore.asStateFlow()

    internal val _selectedProperty = MutableStateFlow<PropertyEntity?>(null)
    var selectedProperty: PropertyEntity?
        get() = _selectedProperty.value
        set(v) { _selectedProperty.value = v }
    val selectedPropertyFlow: StateFlow<PropertyEntity?> = _selectedProperty.asStateFlow()

    internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("HOME"))
    val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()

    internal val _activeVoiceCall = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCall.asStateFlow()"""

if old_block in content:
    content = content.replace(old_block, new_block)
    print("Replaced old_block successfully")
else:
    print("WARNING: old_block not found exactly!")

# Now remove the duplicate block we appended earlier if present
dup_marker = "// --- ADDITIONAL STATEFLOWS AND PROPERTIES FOR FULL COMPATIBILITY ---"
if dup_marker in content:
    parts = content.split(dup_marker)
    # Rebuild: keep parts[0], then keep the stateflows/methods we appended minus duplicates
    appended_part = parts[1]
    # Remove the duplicated lines from appended_part
    lines = appended_part.split("\n")
    filtered_lines = []
    skip = False
    for line in lines:
        if "val triggerRestoreAccountDialog" in line or "var selectedProvider:" in line or "var selectedStore:" in line or "var selectedProperty:" in line or "val activeVoiceCall:" in line:
            continue
        if "_selectedProviderState" in line or "_selectedStoreState" in line or "_selectedPropertyState" in line or "_activeVoiceCallPair" in line:
            continue
        filtered_lines.append(line)
    content = parts[0] + "// --- ADDITIONAL COMPATIBILITY METHODS ---\n" + "\n".join(filtered_lines)

with open("app/src/main/java/com/example/ui/MainViewModel.kt", "w") as f:
    f.write(content)

print("MainViewModel clean up done.")
