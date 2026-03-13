package com.saph.api.video.controller

import com.saph.api.common.ApiResponse
import com.saph.api.job.dto.TranslateRequest
import com.saph.api.job.service.JobService
import com.saph.api.video.service.VideoService
import jakarta.validation.Valid
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@RestController
@RequestMapping
class VideoController(
    private val videoService: VideoService,
    private val jobService: JobService,
) {

    @PostMapping("/api/video/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun upload(
        @RequestPart("file") filePart: FilePart,
        authentication: Authentication,
    ): ResponseEntity<ApiResponse> {
        val memberId = authentication.name.toLong()
        val result = videoService.upload(memberId, filePart)
        return ResponseEntity.ok().body(ApiResponse.success(result))
    }

    @PostMapping("/api/video/translate")
    suspend fun translate(
        @Valid @RequestBody request: TranslateRequest,
        authentication: Authentication,
    ): ApiResponse {
        val memberId = authentication.name.toLong()
        val result = jobService.translate(memberId, request)
        return ApiResponse.success(result)
    }

    @GetMapping("/api/video/status/{jobId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getStatus(
        @PathVariable jobId: Long,
        authentication: Authentication,
    ): Flux<ServerSentEvent<String>> {
        val memberId = authentication.name.toLong()
        return jobService.getStatusStream(jobId, memberId)
    }

    @GetMapping("/api/video/result/{jobId}")
    suspend fun getResult(
        @PathVariable jobId: Long,
        @RequestParam(defaultValue = "translated") type: String,
        authentication: Authentication,
    ): ResponseEntity<ByteArray> {
        val memberId = authentication.name.toLong()
        val path = jobService.getResult(jobId, memberId, type)

        val bytes = Files.readAllBytes(path)
        val filename = path.fileName.toString()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("application/x-subrip"))
            .body(bytes)
    }

    @GetMapping("/api/video/stream/{jobId}")
    suspend fun streamVideo(
        @PathVariable jobId: Long,
        @RequestHeader(value = HttpHeaders.RANGE, required = false) rangeHeader: String?,
        authentication: Authentication,
    ): ResponseEntity<Flux<DataBuffer>> {
        val memberId = authentication.name.toLong()
        val path = jobService.getVideoPath(jobId, memberId)

        val fileSize = Files.size(path)
        val bufferSize = 256 * 1024 // 256KB

        val contentType = when (path.toString().substringAfterLast('.').lowercase()) {
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }

        if (rangeHeader == null) {
            // Return entire file
            val dataFlux = DataBufferUtils.read(path, DefaultDataBufferFactory.sharedInstance, bufferSize)
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
                .body(dataFlux)
        }

        // Parse range header: bytes=start-end
        val range = parseRangeHeader(rangeHeader, fileSize)
        val start = range.first
        val end = range.second
        val contentLength = end - start + 1

        val dataFlux = readRange(path, start, end, bufferSize)

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .header(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileSize")
            .header(HttpHeaders.CONTENT_LENGTH, contentLength.toString())
            .body(dataFlux)
    }

    private fun parseRangeHeader(rangeHeader: String, fileSize: Long): Pair<Long, Long> {
        val range = rangeHeader.removePrefix("bytes=")
        val parts = range.split("-")
        val start = parts[0].toLongOrNull() ?: 0L
        val end = if (parts.size > 1 && parts[1].isNotBlank()) {
            parts[1].toLong().coerceAtMost(fileSize - 1)
        } else {
            fileSize - 1
        }
        return Pair(start.coerceIn(0, fileSize - 1), end.coerceIn(start, fileSize - 1))
    }

    private fun readRange(path: Path, start: Long, end: Long, bufferSize: Int): Flux<DataBuffer> {
        return Flux.using(
            { AsynchronousFileChannel.open(path, StandardOpenOption.READ) },
            { channel ->
                DataBufferUtils.readAsynchronousFileChannel(
                    { channel },
                    start,
                    DefaultDataBufferFactory.sharedInstance,
                    bufferSize,
                ).takeUntil { buffer ->
                    // Stop reading once we've passed the end position
                    false // DataBufferUtils handles position; we rely on content-length header
                }
                .let { flux ->
                    // Take only the bytes up to contentLength
                    val contentLength = end - start + 1
                    var bytesRead = 0L
                    flux.flatMap { buffer ->
                        val remaining = contentLength - bytesRead
                        if (remaining <= 0) {
                            DataBufferUtils.release(buffer)
                            Flux.empty()
                        } else if (buffer.readableByteCount() <= remaining) {
                            bytesRead += buffer.readableByteCount()
                            Flux.just(buffer)
                        } else {
                            val limitedBuffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(remaining.toInt())
                            val bytes = ByteArray(remaining.toInt())
                            buffer.read(bytes)
                            limitedBuffer.write(bytes)
                            DataBufferUtils.release(buffer)
                            bytesRead += remaining
                            Flux.just(limitedBuffer as DataBuffer)
                        }
                    }
                }
            },
            { channel -> channel.close() }
        )
    }
}
