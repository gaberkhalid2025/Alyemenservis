package com.example.ui

fun MainViewModel.setJobBlocked(jobId: String, isBlocked: Boolean, reason: String = "") =
    adminViewModel.setJobBlocked(jobId, isBlocked, reason)
