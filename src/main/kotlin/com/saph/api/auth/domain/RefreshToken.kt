package com.saph.api.auth.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("refresh_tokens")
data class RefreshToken(
    @Id
    val id: Long? = null,

    @Column("member_id")
    val memberId: Long,

    @Column("token")
    val token: String,

    @Column("expires_at")
    val expiresAt: LocalDateTime,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,
)
