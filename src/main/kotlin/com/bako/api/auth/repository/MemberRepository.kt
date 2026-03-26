package com.bako.api.auth.repository

import com.bako.api.auth.domain.Member
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface MemberRepository : ReactiveCrudRepository<Member, Long> {

    @Query("SELECT * FROM members WHERE email = :email LIMIT 1")
    fun findByEmail(email: String): Mono<Member>

    @Query("SELECT id FROM members WHERE email = :email AND status::text != :status LIMIT 1")
    fun findIdByEmailAndStatusNot(email: String, status: String): Mono<Long>

    @Query("SELECT * FROM members WHERE firebase_uid = :firebaseUid LIMIT 1")
    fun findByFirebaseUid(firebaseUid: String): Mono<Member>
}
