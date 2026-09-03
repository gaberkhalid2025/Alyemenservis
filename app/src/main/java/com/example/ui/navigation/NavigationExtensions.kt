package com.example.ui.navigation

import com.example.ui.MainViewModel

fun MainViewModel.navigateToScreen(screen: String) {
    this.navigateTo(screen)
}
