package com.saph.api.job.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.saph.api.job.domain.JobStatus
import com.saph.api.job.repository.JobRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobEventListener(
    private val redisListenerContainer: ReactiveRedisMessageListenerContainer,
    private val jobRepository: JobRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(JobEventListener::class.java)

    @PostConstruct
    fun subscribe() {
        redisListenerContainer
            .receive(PatternTopic("job:done:*"))
            .flatMap { message ->
                val jobId = message.channel.removePrefix("job:done:").toLongOrNull()
                    ?: return@flatMap reactor.core.publisher.Mono.empty()

                val node = runCatching { objectMapper.readTree(message.message) }.getOrNull()
                    ?: return@flatMap reactor.core.publisher.Mono.empty()

                val status = node.get("status")?.asText()

                jobRepository.findById(jobId).flatMap { job ->
                    val updated = if (status == "COMPLETED") {
                        log.info("Job {} COMPLETED, saving SRT paths", jobId)
                        job.copy(
                            status = JobStatus.COMPLETED,
                            originalSrt = node.get("original_srt")?.asText(),
                            translatedSrt = node.get("translated_srt")?.asText(),
                            dualSrt = node.get("dual_srt")?.asText(),
                            completedAt = LocalDateTime.now(),
                        )
                    } else {
                        log.warn("Job {} FAILED: {}", jobId, node.get("error")?.asText())
                        job.copy(
                            status = JobStatus.FAILED,
                            errorMessage = node.get("error")?.asText(),
                            completedAt = LocalDateTime.now(),
                        )
                    }
                    jobRepository.save(updated)
                }
            }
            .doOnError { e -> log.error("JobEventListener error: {}", e.message) }
            .retry()
            .subscribe()
    }
}
