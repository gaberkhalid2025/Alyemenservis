import re

filepath = "app/src/main/java/com/example/viewmodels/AdminViewModel.kt"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("val adminSettings: StateFlow<AdminSettingsEntity> = _adminSettings.asStateFlow()",
"val adminSettings: StateFlow<AdminSettingsEntity> = _adminSettings.asStateFlow()\n    val settings: StateFlow<AdminSettingsEntity> = _adminSettings.asStateFlow()")

with open(filepath, 'w') as f:
    f.write(content)
