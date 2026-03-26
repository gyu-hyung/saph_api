package com.bako.api.job.repository

import com.bako.api.job.domain.Job
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface JobRepository : ReactiveCrudRepository<Job, Long> {

    fun findByIdAndMemberId(id: Long, memberId: Long): Mono<Job>

    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): Flux<Job>

    @Query("SELECT COUNT(*) FROM jobs WHERE status = 'QUEUED' AND id < :jobId")
    fun countQueuedBefore(jobId: Long): Mono<Long>
}
