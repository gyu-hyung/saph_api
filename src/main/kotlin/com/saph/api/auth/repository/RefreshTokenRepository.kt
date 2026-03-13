package com.saph.api.auth.repository

import com.saph.api.auth.domain.RefreshToken
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface RefreshTokenRepository : ReactiveCrudRepository<RefreshToken, Long> {

    fun findByToken(token: String): Mono<RefreshToken>

    fun deleteByMemberId(memberId: Long): Mono<Void>

    fun deleteByToken(token: String): Mono<Void>
}
