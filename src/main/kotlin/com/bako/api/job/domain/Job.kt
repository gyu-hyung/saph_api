package com.bako.api.job.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("jobs")
data class Job(
    @Id
    val id: Long? = null,

    @Column("member_id")
    val memberId: Long,

    @Column("status")
    val status: JobStatus = JobStatus.CREATED,

    @Column("current_step")
    val currentStep: JobStep? = null,

    @Column("progress")
    val progress: Short = 0,

    @Column("video_path")
    val videoPath: String,

    @Column("original_name")
    val originalName: String,

    @Column("video_duration")
    val videoDuration: Int,

    @Column("credit_used")
    val creditUsed: Int,

    @Column("source_lang")
    val sourceLang: String = "auto",

    @Column("target_lang")
    val targetLang: String = "ko",

    @Column("original_srt")
    val originalSrt: String? = null,

    @Column("translated_srt")
    val translatedSrt: String? = null,

    @Column("dual_srt")
    val dualSrt: String? = null,

    @Column("error_message")
    val errorMessage: String? = null,

    @Column("retry_count")
    val retryCount: Short = 0,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,

    @Column("completed_at")
    val completedAt: LocalDateTime? = null,
)

enum class JobStatus {
    CREATED, QUEUED, PROCESSING, COMPLETED, FAILED
}

enum class JobStep {
    AUDIO_EXTRACTION, STT, TRANSLATION, SUBTITLE_BUILD
}
