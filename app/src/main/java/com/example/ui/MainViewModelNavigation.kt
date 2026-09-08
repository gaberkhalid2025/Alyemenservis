package com.example.ui

import android.content.Context
import com.example.ui.*


fun MainViewModel.navigateBack(): Boolean {
    return this.goBack()
}

fun MainViewModel.switchAppLanguage() {
    this.switchLanguage()
}

fun MainViewModel.setAppLanguage(context: Context, lang: String) {
    this.setLanguage(context, lang)
}
