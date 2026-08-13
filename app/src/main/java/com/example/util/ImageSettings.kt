package com.example.util

object ImageSettings {
    var maxQualityPercent: Int = 75
    var maxResolutionDimension: Int = 1080
    var maxFileSizeKB: Int = 1024
    var preferredFormat: String = "JPEG"

    fun updateSettings(quality: Int, resolution: Int, fileSize: Int, format: String) {
        maxQualityPercent = quality
        maxResolutionDimension = resolution
        maxFileSizeKB = fileSize
        preferredFormat = format
    }
}
