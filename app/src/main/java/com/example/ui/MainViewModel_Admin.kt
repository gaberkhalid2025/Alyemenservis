package com.example.ui

import com.example.data.*

fun MainViewModel.verifyAdminOrOwnerPassword(password: String): Boolean {
    return password == "123456" || password == "admin" || password == "owner"
}
