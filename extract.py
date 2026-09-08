import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    lines = f.readlines()

new_viewmodel_lines = lines[:492]
new_viewmodel_lines.append("}\n")

delegates_lines = lines[492:-1] # excluding the last `}`

text = "".join(delegates_lines)

# Only replace `fun ` that are at the first level of indentation (4 spaces) or 0 spaces
text = re.sub(r'^( {0,4})fun\s+([a-zA-Z0-9_]+)\s*\(', r'\1fun MainViewModel.\2(', text, flags=re.MULTILINE)
# Also `val ` and `var ` at 0-4 spaces indentation
text = re.sub(r'^( {0,4})val\s+([a-zA-Z0-9_]+)', r'\1val MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^( {0,4})var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)
text = re.sub(r'^( {0,4})private\s+var\s+([a-zA-Z0-9_]+)', r'\1var MainViewModel.\2', text, flags=re.MULTILINE)

# Fix empty catch blocks
text = text.replace("catch (e: Exception) {}", "catch (e: Exception) { android.util.Log.e(\"MainViewModel\", \"Error: \", e) }")

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

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.writelines(new_viewmodel_lines)

