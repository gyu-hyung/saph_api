package com.bako.api.video.dto

data class UploadResponse(
    val videoId: String,
    val originalName: String,
    val videoPath: String,
    val durationSec: Int,
    val requiredCreditMin: Int,
    val fileSizeMB: Double,
)
