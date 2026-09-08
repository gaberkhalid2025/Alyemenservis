import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    lines = f.readlines()

text = "".join(lines[492:1602]) # 0-indexed, so 492 is line 493

text = re.sub(r'^(\s*)fun\s+([a-zA-Z0-9_]+)\s*\(', r'\1fun MainViewModel.\2(', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)val\s+([a-zA-Z0-9_]+)', r'\1val MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^(\s*)private\s+var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)

imports = """package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.data.models.*
import com.example.data.*
import java.util.UUID
import com.example.utils.AppResult
import com.example.ui.viewmodels.SettingsViewModel.ChatParticipantType
import com.example.ui.viewmodels.BookingFormFields
import com.example.ui.viewmodels.BookingDistributionMode
import com.example.ui.viewmodels.BookingStatus
import com.example.utils.LocaleManager

"""

with open('app/src/main/java/com/example/ui/MainViewModelDelegates.kt', 'w') as f:
    f.write(imports + text)

