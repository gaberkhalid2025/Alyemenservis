package com.example.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


fun MainViewModel.createSystemBackup() {
    // Create system backup
}

fun MainViewModel.restoreSystemFromBackup() {
    // Restore system backup
}

fun MainViewModel.triggerManualSync() {
    refreshData()
}
