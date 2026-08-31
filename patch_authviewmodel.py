import re

filepath = "app/src/main/java/com/example/viewmodels/AuthViewModel.kt"
with open(filepath, 'r') as f:
    content = f.read()

props = """
    val currentUserId: StateFlow<String> = _authState.map { it.userId }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val currentUserName: StateFlow<String> = _authState.map { it.userName }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val currentUserPhone: StateFlow<String> = _authState.map { it.userPhone }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "")
    val isOnline: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true).asStateFlow()
    val isProviderUser: StateFlow<Boolean> = _authState.map { it.userRole == "PROVIDER" }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, false)
"""

if "currentUserId" not in content:
    content = content.replace("val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()", 
    "val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()\n" + props)
    content = content.replace("import kotlinx.coroutines.flow.asStateFlow", "import kotlinx.coroutines.flow.asStateFlow\nimport kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.stateIn")

with open(filepath, 'w') as f:
    f.write(content)
import re

filepath = "app/src/main/java/com/example/viewmodels/AuthViewModel.kt"
with open(filepath, 'r') as f:
    content = f.read()

props = """
    val triggerRestoreAccountDialog = MutableStateFlow(false)
"""

if "triggerRestoreAccountDialog" not in content:
    content = content.replace("val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()", 
    "val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()\n" + props)

with open(filepath, 'w') as f:
    f.write(content)
