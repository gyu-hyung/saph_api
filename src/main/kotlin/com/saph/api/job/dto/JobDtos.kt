package com.saph.api.job.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class TranslateRequest(
    @field:NotBlank(message = "videoPath is required")
    val videoPath: String,

    val sourceLang: String = "auto",

    @field:NotBlank(message = "targetLang is required")
    val targetLang: String = "ko",
)

data class TranslateResponse(
    val jobId: Long,
    val status: String,
    val creditUsed: Int,
    val creditBalance: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JobSummary(
    val jobId: Long,
    val status: String,
    val originalName: String,
    val durationSec: Int,
    val creditUsed: Int,
    val sourceLang: String,
    val targetLang: String,
    val detectedLang: String? = null,
    val errorMessage: String? = null,
    val createdAt: LocalDateTime?,
    val completedAt: LocalDateTime? = null,
)

sealed class SsePayload {
    data class Progress(
        val jobId: Long,
        val step: String,
        val percent: Int,
        val message: String,
    ) : SsePayload()

    data class Queued(
        val jobId: Long,
        val queuePosition: Int,
        val estimatedWaitSec: Int,
        val message: String,
    ) : SsePayload()

    data class Completed(
        val jobId: Long,
        val status: String = "COMPLETED",
        val originalSrt: String?,
        val translatedSrt: String?,
        val dualSrt: String?,
    ) : SsePayload()

    data class Failed(
        val jobId: Long,
        val status: String = "FAILED",
        val error: String?,
    ) : SsePayload()
}
