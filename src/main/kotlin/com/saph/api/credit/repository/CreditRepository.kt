package com.saph.api.credit.repository

import com.saph.api.credit.domain.Credit
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface CreditRepository : ReactiveCrudRepository<Credit, Long> {

    fun findByMemberId(memberId: Long): Mono<Credit>
}
