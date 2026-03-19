package com.saph.api.video.service

import com.saph.api.common.ApiException
import com.saph.api.config.AppProperties
import com.saph.api.video.dto.UploadResponse
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.math.ceil
import kotlin.math.roundToInt

@Service
class VideoService(
    private val appProperties: AppProperties,
) {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("mp4", "mov")
        private const val MAX_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB
        private const val MAX_DURATION_SEC = 300 // 5 minutes = 300 seconds
    }

    suspend fun upload(memberId: Long, filePart: FilePart): UploadResponse {
        val originalName = filePart.filename()
        val extension = originalName.substringAfterLast('.', "").lowercase()

        if (extension !in ALLOWED_EXTENSIONS) {
            throw ApiException.badRequest(
                "UNSUPPORTED_FORMAT",
                "Only mp4 and mov files are allowed. Got: $extension"
            )
        }

        val videosDir = Path.of(appProperties.storage.videosDir)
        if (!videosDir.exists()) {
            Files.createDirectories(videosDir)
        }

        val videoId = UUID.randomUUID().toString()
        val targetPath = videosDir.resolve("$videoId.$extension")

        // Write file to disk
        DataBufferUtils.write(
            filePart.content(),
            targetPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
        ).then().awaitSingleOrNull()

        // Check file size after save
        val fileSize = targetPath.fileSize()
        if (fileSize > MAX_SIZE_BYTES) {
            Files.deleteIfExists(targetPath)
            throw ApiException.badRequest(
                "FILE_TOO_LARGE",
                "File size exceeds 100MB limit. Got: ${fileSize / 1024 / 1024}MB"
            )
        }

        // Get duration via ffprobe
        val durationSec = ffprobe(targetPath.toString())

        if (durationSec > MAX_DURATION_SEC) {
            Files.deleteIfExists(targetPath)
            throw ApiException.badRequest(
                "VIDEO_TOO_LONG",
                "Video duration exceeds 300 seconds. Got: ${durationSec}s"
            )
        }

        val requiredCreditMin = ceil(durationSec / 60.0).toInt()
        val fileSizeMB = fileSize.toDouble() / 1024 / 1024

        return UploadResponse(
            videoId = videoId,
            originalName = originalName,
            videoPath = targetPath.toAbsolutePath().toString(),
            durationSec = durationSec,
            requiredCreditMin = requiredCreditMin,
            fileSizeMB = (fileSizeMB * 100).roundToInt() / 100.0,
        )
    }

    fun findVideoPath(videoId: String): String {
        val videosDir = Path.of(appProperties.storage.videosDir)
        if (!videosDir.exists()) {
            throw ApiException.badRequest("VIDEO_NOT_FOUND", "Video directory not found")
        }

        val matchingFiles = videosDir.listDirectoryEntries()
            .filter { it.fileName.toString().startsWith(videoId) }
            .filter { it.fileName.toString().substringAfterLast('.', "").lowercase() in ALLOWED_EXTENSIONS }

        if (matchingFiles.isEmpty()) {
            throw ApiException.badRequest("VIDEO_NOT_FOUND", "Video not found: $videoId")
        }

        return matchingFiles.first().toAbsolutePath().toString()
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
            throw ApiException.internalError("FFPROBE_ERROR", "Failed to analyze video: ${e.message}")
        }
    }
}
