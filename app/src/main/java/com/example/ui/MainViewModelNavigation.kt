package com.example.ui

import android.content.Context

fun MainViewModel.navigateToScreen(screen: String) {
    this.navigateTo(screen)
}

fun MainViewModel.navigateBack(): Boolean {
    return this.goBack()
}

fun MainViewModel.switchAppLanguage() {
    this.switchLanguage()
}

fun MainViewModel.setAppLanguage(context: Context, lang: String) {
    this.setLanguage(context, lang)
}
