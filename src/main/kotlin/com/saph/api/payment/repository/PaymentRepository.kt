package com.saph.api.payment.repository

import com.saph.api.payment.domain.Payment
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface PaymentRepository : ReactiveCrudRepository<Payment, Long> {

    fun findByPgOrderId(pgOrderId: String): Mono<Payment>
}
