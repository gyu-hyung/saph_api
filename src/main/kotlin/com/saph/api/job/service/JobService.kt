package com.saph.api.job.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.saph.api.common.ApiException
import com.saph.api.credit.service.CreditService
import com.saph.api.job.domain.Job
import com.saph.api.job.domain.JobStatus
import com.saph.api.job.dto.JobSummary
import com.saph.api.job.dto.SsePayload
import com.saph.api.job.dto.TranslateRequest
import com.saph.api.job.dto.TranslateResponse
import com.saph.api.job.repository.JobRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.connection.ReactiveSubscription
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.springframework.http.codec.ServerSentEvent
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.math.ceil

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val creditService: CreditService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val redisListenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    suspend fun translate(memberId: Long, request: TranslateRequest): TranslateResponse {
        // Validate file exists
        val videoFile = Path.of(request.videoPath)
        if (!videoFile.exists()) {
            throw ApiException.badRequest("VIDEO_NOT_FOUND", "Video file not found: ${request.videoPath}")
        }

        // Get duration via ffprobe
        val durationSec = ffprobe(request.videoPath)
        val requiredMinutes = ceil(durationSec / 60.0).toInt()

        // Deduct credits - this uses SELECT FOR UPDATE inside
        // We save the job first as CREATED to get an ID, then deduct
        val originalName = videoFile.fileName.toString()

        val job = jobRepository.save(
            Job(
                memberId = memberId,
                status = JobStatus.CREATED,
                videoPath = request.videoPath,
                originalName = originalName,
                videoDuration = durationSec,
                creditUsed = requiredMinutes,
                sourceLang = request.sourceLang,
                targetLang = request.targetLang,
            )
        ).awaitSingle()

        // Deduct credits with pessimistic lock
        val remainingBalance = try {
            creditService.deduct(memberId, requiredMinutes, job.id!!)
        } catch (e: ApiException) {
            // Rollback job creation on credit failure
            jobRepository.deleteById(job.id!!).awaitSingleOrNull()
            throw e
        }

        // Publish to Redis Stream
        val streamKey = "stream:jobs"
        try {
            redisTemplate.opsForStream<String, String>()
                .add(
                    streamKey,
                    mapOf(
                        "jobId" to job.id.toString(),
                        "videoPath" to request.videoPath,
                        "sourceLang" to request.sourceLang,
                        "targetLang" to request.targetLang,
                    )
                )
                .awaitSingle()
        } catch (e: Exception) {
            throw ApiException.internalError("MQ_PUBLISH_FAILED", "번역 큐 등록에 실패했습니다. 다시 시도해주세요.")
        }

        // Update job to QUEUED
        updateJobStatus(job.id!!, JobStatus.QUEUED)

        return TranslateResponse(
            jobId = job.id,
            status = JobStatus.QUEUED.name,
            creditUsed = requiredMinutes,
            creditBalance = remainingBalance,
        )
    }

    fun getStatusStream(jobId: Long, memberId: Long): Flux<ServerSentEvent<String>> {
        return Flux.defer {
            jobRepository.findByIdAndMemberId(jobId, memberId)
                .switchIfEmpty(Mono.error(ApiException.notFound("JOB_NOT_FOUND", "Job not found: $jobId")))
                .flatMapMany { job ->
                    val currentStatus = job.status

                    // If already terminal, emit final event immediately
                    if (currentStatus == JobStatus.COMPLETED) {
                        val payload = SsePayload.Completed(
                            jobId = jobId,
                            originalSrt = job.originalSrt,
                            translatedSrt = job.translatedSrt,
                            dualSrt = job.dualSrt,
                        )
                        return@flatMapMany Flux.just(
                            ServerSentEvent.builder<String>()
                                .event("completed")
                                .data(objectMapper.writeValueAsString(payload))
                                .build()
                        )
                    }

                    if (currentStatus == JobStatus.FAILED) {
                        val payload = SsePayload.Failed(
                            jobId = jobId,
                            error = job.errorMessage,
                        )
                        return@flatMapMany Flux.just(
                            ServerSentEvent.builder<String>()
                                .event("failed")
                                .data(objectMapper.writeValueAsString(payload))
                                .build()
                        )
                    }

                    // Subscribe to Redis Pub/Sub channels
                    val progressChannel = "job:progress:$jobId"
                    val doneChannel = "job:done:$jobId"

                    val progressFlux = redisListenerContainer
                        .receive(ChannelTopic(progressChannel))
                        .map { message ->
                            val body = message.message
                            ServerSentEvent.builder<String>()
                                .event("progress")
                                .data(body)
                                .build()
                        }

                    val doneFlux = redisListenerContainer
                        .receive(ChannelTopic(doneChannel))
                        .flatMap { message ->
                            val body = message.message
                            val node = objectMapper.readTree(body)
                            val status = node.get("status")?.asText()
                            val event = if (status == "COMPLETED") "completed" else "failed"
                            Flux.just(
                                ServerSentEvent.builder<String>()
                                    .event(event)
                                    .data(body)
                                    .build()
                            )
                        }
                        .take(1)

                    // Queue position ticker (every 5s) while in QUEUED state
                    val queuePositionFlux: Flux<ServerSentEvent<String>> = if (currentStatus == JobStatus.QUEUED) {
                        Flux.interval(Duration.ofSeconds(5))
                            .flatMap {
                                jobRepository.findById(jobId)
                                    .flatMap { updatedJob ->
                                        if (updatedJob.status == JobStatus.QUEUED) {
                                            jobRepository.countQueuedBefore(jobId)
                                                .map { countBefore ->
                                                    val position = (countBefore + 1).toInt()
                                                    val estimatedWaitSec = position * 120
                                                    val payload = SsePayload.Queued(
                                                        jobId = jobId,
                                                        queuePosition = position,
                                                        estimatedWaitSec = estimatedWaitSec,
                                                        message = "${position}번째 대기 중입니다. 약 ${estimatedWaitSec / 60}분 후 시작됩니다.",
                                                    )
                                                    ServerSentEvent.builder<String>()
                                                        .event("queued")
                                                        .data(objectMapper.writeValueAsString(payload))
                                                        .build()
                                                }
                                        } else {
                                            // Job started processing, stop queue position ticks
                                            Mono.empty()
                                        }
                                    }
                            }
                    } else {
                        Flux.empty()
                    }

                    // Merge all streams; doneFlux terminates the overall stream
                    Flux.merge(progressFlux, queuePositionFlux)
                        .takeUntilOther(doneFlux)
                        .mergeWith(doneFlux)
                }
        }
    }

    suspend fun getResult(jobId: Long, memberId: Long, type: String): Path {
        val job = jobRepository.findByIdAndMemberId(jobId, memberId).awaitSingleOrNull()
            ?: throw ApiException.notFound("JOB_NOT_FOUND", "Job not found: $jobId")

        if (job.status != JobStatus.COMPLETED) {
            throw ApiException.badRequest("JOB_NOT_COMPLETED", "Job is not completed yet: ${job.status}")
        }

        val filePath = when (type.lowercase()) {
            "original" -> job.originalSrt
            "translated" -> job.translatedSrt
            "dual" -> job.dualSrt
            else -> throw ApiException.badRequest("INVALID_TYPE", "Invalid result type: $type. Use original, translated, or dual")
        } ?: throw ApiException.notFound("FILE_NOT_FOUND", "Result file not found for type: $type")

        val path = Path.of(filePath)
        if (!path.exists()) {
            throw ApiException.notFound("FILE_NOT_FOUND", "Result file does not exist: $filePath")
        }

        return path
    }

    suspend fun getVideoPath(jobId: Long, memberId: Long): Path {
        val job = jobRepository.findByIdAndMemberId(jobId, memberId).awaitSingleOrNull()
            ?: throw ApiException.notFound("JOB_NOT_FOUND", "Job not found: $jobId")

        val path = Path.of(job.videoPath)
        if (!path.exists()) {
            throw ApiException.notFound("FILE_NOT_FOUND", "Video file not found for job: $jobId")
        }

        return path
    }

    suspend fun listJobs(memberId: Long): List<JobSummary> {
        return jobRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .map { job ->
                JobSummary(
                    jobId = job.id!!,
                    status = job.status.name,
                    originalName = job.originalName,
                    durationSec = job.videoDuration,
                    creditUsed = job.creditUsed,
                    sourceLang = job.sourceLang,
                    targetLang = job.targetLang,
                    errorMessage = job.errorMessage,
                    createdAt = job.createdAt,
                    completedAt = job.completedAt,
                )
            }
            .collectList()
            .awaitSingle()
    }

    private suspend fun updateJobStatus(jobId: Long, status: JobStatus) {
        val job = jobRepository.findById(jobId).awaitSingleOrNull() ?: return
        jobRepository.save(job.copy(status = status)).awaitSingle()
    }

    fun ffprobe(filePath: String): Int {
        return try {
            val process = ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                filePath
            )
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            output.toDoubleOrNull()?.toInt() ?: 0
        } catch (e: Exception) {
            throw ApiException.internalError("FFPROBE_ERROR", "Failed to get video duration: ${e.message}")
        }
    }
}
