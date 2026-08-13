package com.example.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


fun MainViewModel.logCall(type: String, target: String) {
    // Log call activity
}

fun MainViewModel.getCurrentTimestampString(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}
